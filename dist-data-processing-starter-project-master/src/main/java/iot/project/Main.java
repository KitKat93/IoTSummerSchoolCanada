package iot.project;

import java.awt.Desktop;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.util.logging.Logging;
import iot.project.distanceCalculation.CommandLineOptions;
import iot.project.distanceCalculation.GraphHopperHelper;

public class Main {
	static int webServerPort = 8080;
	static Logger log;
	private static JSONObject currentObject;

	private static HashMap<Integer, ArrayList<Passenger>> busPassengerRevenues;
	private static HashMap<Integer, ArrayList<Passenger>> previousBusPassengerRevenues;
	private static HashMap<Integer, ArrayList<Passenger>> finishedPassengers;

	private static GraphHopperHelper distanceCalculationHelper;
	private static JSONArray featuresList = new JSONArray();

	static {
		Logging.setLoggingDefaults();
	}

	public static void main(String[] args) throws Exception {
		// Obtain an instance of a logger for this class
		log = LoggerFactory.getLogger(Main.class);

		// Start a web server
		setupWebServer(webServerPort);
		log.info("Web server started on port " + webServerPort);
		log.info("Open http://localhost:" + webServerPort + " and/or http://localhost:" + webServerPort + "/hello");

		setupGraphHopper(args);

		try {
			readCSVFile();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initializes and sets the urls for the spark webserver.
	 * 
	 * @param webServerPort
	 *            The port on which the webserver should be opened.
	 */
	public static void setupWebServer(int webServerPort) {
		// Set the web server's port
		spark.Spark.port(webServerPort);

		// Serve static files from src/main/resources/webroot
		spark.Spark.staticFiles.location("/webroot");

		spark.Spark.get("/data", (req, res) -> currentObject.toJSONString());

		spark.Spark.get("/tsv", (req, res) -> createPassengerTSVFile());
		spark.Spark.get("/buses", (req, res) -> createBusTSVFile());

		// Wait for server to be initialized
		spark.Spark.awaitInitialization();

		try {
			Desktop.getDesktop().browse(new URL("http://localhost:8080").toURI());
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Creates the TSV file representation of the current passenger revenues and
	 * returns it as a string.
	 * 
	 * @return returns the String representation of the tsv file for the
	 *         passenger revenues.
	 * @throws IOException
	 */
	private static String createPassengerTSVFile() throws IOException {

		StringWriter writer = new StringWriter();
		writer.write("passenger\trevenue" + "\n");

		Set<Entry<Integer, ArrayList<Passenger>>> set = finishedPassengers.entrySet();
		Iterator<Entry<Integer, ArrayList<Passenger>>> i = set.iterator();
		while (i.hasNext()) {
			ArrayList<Passenger> finishedP = ((Entry<Integer, ArrayList<Passenger>>) i.next()).getValue();
			for (Passenger p : finishedP) {
				writer.append(String.valueOf(p.getId()) + "\t" + String.format("%.2f", p.getRevenue()).replace(",", ".") + "\n");
			}
		}

		return writer.toString();
	}

	/**
	 * Creates the TSV file representation of the current overall bus revenues
	 * and returns it as a string.
	 * 
	 * @return returns the String representation of the tsv file for the overall
	 *         bus revenues.
	 * @throws IOException
	 */
	private static String createBusTSVFile() throws IOException {

		StringWriter writer = new StringWriter();
		writer.write("bus\trevenue" + "\n");

		Set<Entry<Integer, ArrayList<Passenger>>> set = finishedPassengers.entrySet();
		Iterator<Entry<Integer, ArrayList<Passenger>>> i = set.iterator();
		while (i.hasNext()) {
			Entry<Integer, ArrayList<Passenger>> finishedP = ((Entry<Integer, ArrayList<Passenger>>) i.next());
			double sumOfBus = 0;
			for (Passenger p : finishedP.getValue()) {
				sumOfBus += p.getRevenue();
			}
			writer.append(String.valueOf(finishedP.getKey()) + "\t" + String.format("%.2f", sumOfBus).replace(",", ".") + "\n");
		}
		return writer.toString();
	}

	/**
	 * Initializes the GraphHopper instance.
	 * 
	 * @param args
	 *            The String arguments referring to the pbf file needed for the
	 *            graphhopper initialization.
	 * @throws Exception
	 */
	private static void setupGraphHopper(String[] args) throws Exception {
		CommandLineOptions options = parseCmdLineOptions(args);

		if (options.verbose) {
			org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
			log.debug("Debug enabled");
		}

		distanceCalculationHelper = new GraphHopperHelper(options.osmFile);
	}

	private static void printHelpAndExit(CmdLineParser parser) {
		System.err.print("Usage: java " + Main.class.getCanonicalName());
		parser.printSingleLineUsage(System.err);
		System.err.println();
		parser.printUsage(System.err);
		System.exit(1);
	}

	private static CommandLineOptions parseCmdLineOptions(final String[] args) {
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
			if (options.help)
				printHelpAndExit(parser);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			printHelpAndExit(parser);
		}

		return options;
	}

	/**
	 * Handles the csv file reading and streaming using spark and generates the
	 * GeoJSON objects.
	 * 
	 * @throws IOException
	 */
	private static void readCSVFile() throws IOException {

		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(
				new FileReader("src/main/resources/webroot/sim-data-out-oshawa-small.csv"));
		br.readLine();
		AtomicInteger lastTimeStamp = new AtomicInteger(-1);

		ServerSocketSource<String> dataSource = new ServerSocketSource<>(() -> {
			String line = "";
			try {
				line = br.readLine();
			} catch (Exception e) {
				e.printStackTrace();
			}
			String[] lineSplit = line.split(",");
			AtomicInteger currentTimeStamp = new AtomicInteger(Integer.valueOf(lineSplit[0]));
			if (currentTimeStamp.get() != lastTimeStamp.get()) {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					e.printStackTrace();
				}
				lastTimeStamp.set(currentTimeStamp.get());
			}
			return line;
		}, () -> 0);

		SparkConf conf = new SparkConf().setAppName("JavaNetworkWordCount").setMaster("local[2]");

		JavaStreamingContext ssc = new JavaStreamingContext(conf, Durations.milliseconds(500));
		ssc.remember(Durations.milliseconds(1));

		JavaReceiverInputDStream<String> lines = ssc.socketTextStream("localhost", dataSource.getLocalPort(),
				StorageLevels.MEMORY_AND_DISK_SER);

		lines.foreachRDD(new VoidFunction() {
			@Override
			public void call(Object arg0) throws Exception {

				JavaRDD<String> rdd = (JavaRDD<String>) arg0;

				if (rdd.count() > 0) {

					JSONObject featureCollection = new JSONObject();
					featureCollection.put("type", "FeatureCollection");
					featuresList.clear();
					rdd.foreach(new VoidFunction<String>() {
						@Override
						public void call(String row) throws Exception {
							extractBusPassengerData(row);
							JSONObject rowObject = generateBusGeoJSONObject(row);
							featuresList.add(rowObject);
						}
					});
					featureCollection.put("features", featuresList);
					currentObject = featureCollection;
					previousBusPassengerRevenues = busPassengerRevenues;
				}
			}
		});

		ssc.start();

		ssc.awaitTermination();
		ssc.close();
		dataSource.stop();
	}

	/**
	 * Generates the JSON object for the current string row.
	 * 
	 * @param row
	 *            The string representation of the current csv file row.
	 * @return The JSON object genrated for that row.
	 */
	private static JSONObject generateBusGeoJSONObject(String row) {
		String[] rowSplit = row.split(",");

		JSONObject feature = new JSONObject();
		feature.put("type", "Feature");

		JSONArray coord = new JSONArray();
		coord.add(Double.valueOf(rowSplit[4]));
		coord.add(Double.valueOf(rowSplit[3]));

		JSONObject point = new JSONObject();
		point.put("coordinates", coord);
		point.put("type", "Point");

		feature.put("geometry", point);

		JSONObject properties = new JSONObject();
		properties.put("busID", Integer.valueOf(rowSplit[1]));

		feature.put("properties", properties);

		return feature;
	}

	/**
	 * Extracts the bus passenger data from the row and handles the hopping
	 * on/off of the bus by the passengers.
	 * 
	 * @param row
	 *            The string representation of the current csv file row.
	 */
	private static void extractBusPassengerData(String row) {

		if (busPassengerRevenues == null) {
			busPassengerRevenues = new HashMap<Integer, ArrayList<Passenger>>();
			previousBusPassengerRevenues = new HashMap<Integer, ArrayList<Passenger>>();
		}

		if (finishedPassengers == null) {
			finishedPassengers = new HashMap<Integer, ArrayList<Passenger>>();
		}

		String[] rowSplit = row.split(",");
		int busID = Integer.valueOf(rowSplit[1]);
		String passengersOfBus = rowSplit[15];
		String[] passengerSplit = passengersOfBus.split(";");

		if (busPassengerRevenues.containsKey(busID)) {
			ArrayList<Passenger> passengers = busPassengerRevenues.get(busID);
			boolean passengerContained = false;

			// passenger left
			if (passengers.size() >= passengerSplit.length) {
				for (Passenger p : passengers) {
					for (String s : passengerSplit) {
						if (Integer.valueOf(s) == p.getId()) {
							passengerContained = true;
							break;
						}
					}
					if (!passengerContained) {

						// passenger not in bus, check if it was in the previous
						// one and therefore left it
						if ((previousBusPassengerRevenues.get(busID)) != null
								&& previousBusPassengerRevenues.get(busID).contains(p)) {

							// in previous bus but not in the current one, so
							// remove and calculate the revenue
							int idx = previousBusPassengerRevenues.get(busID).indexOf(p);
							Passenger prev = previousBusPassengerRevenues.get(busID).get(idx);
							prev.calculateRevenue(
									new Point2D.Double(Double.valueOf(rowSplit[3]), Double.valueOf(rowSplit[4])),
									distanceCalculationHelper);

							if (finishedPassengers.containsKey(busID)) {
								ArrayList<Passenger> fp = finishedPassengers.get(busID);
								fp.add(prev);
								finishedPassengers.replace(busID, fp);
							} else {
								ArrayList<Passenger> fp = new ArrayList<Passenger>();
								fp.add(prev);
								finishedPassengers.put(busID, fp);
							}

							previousBusPassengerRevenues.get(busID).remove(idx);
						}
					}
				}
			} else { // s.length >= p.length, someone got on the bus or people
						// count is the same
				for (String s : passengerSplit) {
					for (Passenger p : passengers) {
						if (Integer.valueOf(s) == p.getId()) {
							passengerContained = true;
							break;
						}
					}
					if (!passengerContained) {
						// passenger currently in bus, check if entered or not
						Passenger newPassenger = new Passenger(Integer.valueOf(s));
						if ((previousBusPassengerRevenues.get(busID)) != null
								&& !previousBusPassengerRevenues.get(busID).contains(newPassenger)
								&& newPassenger.getId() != -1) {

							// not in previous bus and not in current bus, so
							// add to the passenger list
							passengers.add(new Passenger(Integer.valueOf(s),
									new Point2D.Double(Double.valueOf(rowSplit[3]), Double.valueOf(rowSplit[4]))));
						}
					}
				}
			}
		} else {
			// check if bus was removed/has finished his tour
			if (previousBusPassengerRevenues.containsKey(busID)) {
				previousBusPassengerRevenues.remove(busID);
			} else {
				ArrayList<Passenger> passengers = new ArrayList<Passenger>();

				for (String s : passengerSplit) {
					log.info("Passenger " + s + " inserted");
					if (Integer.valueOf(s) != -1) {
						passengers.add(new Passenger(Integer.valueOf(s),
								new Point2D.Double(Double.valueOf(rowSplit[3]), Double.valueOf(rowSplit[4]))));
					}
				}
				busPassengerRevenues.put(busID, passengers);
			}
		}
	}
}

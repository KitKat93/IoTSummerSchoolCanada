package iot.project.distanceCalculation;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.util.EncodingManager;

public class GraphHopperHelper {
	Logger log = LoggerFactory.getLogger(GraphHopperHelper.class);
	private GraphHopper hopper;

	public GraphHopperHelper(File osmFile) throws IOException {
		// create one GraphHopper instance
		this.hopper = new GraphHopper().forServer();
		hopper.setOSMFile(osmFile.toString());

		File tempDirectory = Files.createTempDir();
		FileUtils.forceDeleteOnExit(tempDirectory);
		log.debug("Using temp dir {}", tempDirectory.toString());
		hopper.setGraphHopperLocation(tempDirectory.toString());
		hopper.setEncodingManager(new EncodingManager("car"));
		hopper.importOrLoad();
	}

	public PathWrapper route(double fromLat, double fromLon, double toLat, double toLon) throws Exception {
		GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).setWeighting("fastest").setVehicle("car")
				.setLocale(Locale.US);

		GHResponse rsp = hopper.route(req);

		if (rsp.hasErrors()) {
			String errorMessage = "";
			for (Throwable t : rsp.getErrors())
				errorMessage += t.toString();

			throw new Exception(errorMessage);
		}

		return rsp.getBest();
	}
}

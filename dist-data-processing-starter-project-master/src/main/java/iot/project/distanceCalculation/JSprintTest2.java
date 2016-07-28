package iot.project.distanceCalculation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import jsprit.analysis.toolbox.Plotter;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.box.Jsprit;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.VehicleRoutingProblem.Builder;
import jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.VehicleImpl;
import jsprit.core.problem.vehicle.VehicleType;
import jsprit.core.problem.vehicle.VehicleTypeImpl;
import jsprit.core.reporting.SolutionPrinter;
import jsprit.core.util.FastVehicleRoutingTransportCostsMatrix;
import jsprit.core.util.Solutions;

public class JSprintTest2 {

	static {
		Logging.setLoggingDefaults(LogLevel.DEBUG, "[%-5p; %c{1}::%M] %m%n");
	}

	public static void main(String[] args) {
		Logger log = LoggerFactory.getLogger(JSprintTest2.class);

		int maxLocations = 100;
		int maxServices = 2;
		boolean isSymmetric = true;

		VehicleType type = VehicleTypeImpl.Builder.newInstance("type")
				.addCapacityDimension(0, maxServices)
				.setCostPerDistance(50)
				.setCostPerTransportTime(2)
				.setFixedCost(100)
				.build();

		FastVehicleRoutingTransportCostsMatrix.Builder matrixBuilder = FastVehicleRoutingTransportCostsMatrix.Builder
				.newInstance(maxLocations, isSymmetric);

		// double[] min = { 49.367898656399326, 8.610191345214844 }, max = { 49.413876997493894, 8.701858520507812 };
		double[] min = { 0, 0 }, max = { 100, 100 };

		RandomLocations randomLocations = new RandomLocations(min, max);
		List<Location> locations = new LinkedList<>();

		for (int i = 0; i < maxLocations; ++i) {
			Location location = randomLocations.next(i);
			log.debug("Location[{}] = {}", i, location);
			locations.add(location);
		}

		Random r = new Random();

		VehicleImpl vehicle = VehicleImpl.Builder.newInstance("vehicle")
				.setStartLocation(locations.get(r.nextInt(locations.size())))
				.setType(type)
				.build();

		log.debug("Vehicle: {}", vehicle);

		for (int i = 0; i < maxLocations; ++i) {
			for (int j = 0; j < maxLocations; ++j) {
				if (i == j || r.nextDouble() < 0.9)
					continue;
				int distance = r.nextInt(5000);
				matrixBuilder.addTransportDistance(i, j, distance);
				log.debug("Adding cost {} -> {} of {}", i, j, distance);
			}
		}
		FastVehicleRoutingTransportCostsMatrix costMatrix = matrixBuilder.build();

		List<Service> services = new LinkedList<>();

		for (int i = 0; i < maxServices; ++i) {
			Service service = Service.Builder.newInstance("" + i)
					.addSizeDimension(0, 1)
					.setLocation(locations.get(r.nextInt(locations.size())))
					.build();
			log.debug("New service {}", service);
			services.add(service);
		}

		Builder builder = VehicleRoutingProblem.Builder.newInstance();
		builder.setFleetSize(FleetSize.FINITE).setRoutingCost(costMatrix).addVehicle(vehicle);

		for (Service service : services)
			builder.addJob(service);

		VehicleRoutingProblem vrp = builder.build();

		VehicleRoutingAlgorithm vra = Jsprit.createAlgorithm(vrp);
		Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();

		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

		for (VehicleRoute route : bestSolution.getRoutes()) {
			log.debug("Route : {}", route.toString());

			log.debug("---Activities--------------------------");
			for (TourActivity a : route.getActivities()) {
				log.debug("Activity: {}", a);
			}

			log.debug("---TourActivities--------------------------");
			for (TourActivity a : route.getTourActivities().getActivities()) {
				log.debug("TourActivity: {}", a);
			}
		}

		log.debug("Got {} unassigned jobs", bestSolution.getUnassignedJobs().size());

		SolutionPrinter.print(bestSolution);

		new Plotter(vrp, bestSolution).plot("yo.png", "po");
	}

}

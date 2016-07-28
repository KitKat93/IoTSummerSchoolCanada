package iot.project.distanceCalculation;

import java.util.Collection;

import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import jsprit.analysis.toolbox.Plotter;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.box.Jsprit;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.vehicle.VehicleImpl;
import jsprit.core.problem.vehicle.VehicleType;
import jsprit.core.problem.vehicle.VehicleTypeImpl;
import jsprit.core.reporting.SolutionPrinter;
import jsprit.core.util.Solutions;
import jsprit.core.util.VehicleRoutingTransportCostsMatrix;

public class JSprintTest {

	static {
		Logging.setLoggingDefaults(LogLevel.INFO, "[%-5p; %c{1}::%M] %m%n");
	}

	public static void main(String[] args) {
		VehicleType type = VehicleTypeImpl.Builder.newInstance("type")
				.addCapacityDimension(0, 2)
				.setCostPerDistance(1)
				.setCostPerTransportTime(2)
				.setFixedCost(100)
				.build();

		VehicleImpl vehicle = VehicleImpl.Builder.newInstance("vehicle").setStartLocation(Location.newInstance("0")).setType(type).build();

		Service s1 = Service.Builder.newInstance("1").addSizeDimension(0, 1).setLocation(Location.newInstance("1")).build();
		Service s2 = Service.Builder.newInstance("2").addSizeDimension(0, 1).setLocation(Location.newInstance("2")).build();
		Service s3 = Service.Builder.newInstance("3").addSizeDimension(0, 1).setLocation(Location.newInstance("3")).build();

		VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix.Builder.newInstance(true);
		costMatrixBuilder.addTransportDistance("0", "1", 10.0);
		costMatrixBuilder.addTransportDistance("0", "2", 20.0);
		costMatrixBuilder.addTransportDistance("0", "3", 5.0);
		costMatrixBuilder.addTransportDistance("1", "2", 4.0);
		costMatrixBuilder.addTransportDistance("1", "3", 1.0);
		costMatrixBuilder.addTransportDistance("2", "3", 2.0);

		VehicleRoutingTransportCosts costMatrix = costMatrixBuilder.build();

		VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance()
				.setFleetSize(FleetSize.INFINITE)
				.setRoutingCost(costMatrix)
				.addVehicle(vehicle)
				.addJob(s1)
				.addJob(s2)
				.addJob(s3)
				.build();

		VehicleRoutingAlgorithm vra = Jsprit.createAlgorithm(vrp);
		Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();

		SolutionPrinter.print(Solutions.bestOf(solutions));

		new Plotter(vrp, Solutions.bestOf(solutions)).plot("yo.png", "po");
	}
}

package iot.project.distanceCalculation;

import java.util.Random;

import jsprit.core.problem.Location;
import jsprit.core.util.Coordinate;

public class RandomLocations {
	private static final Random r = new Random();
	private double[] min;
	private double[] max;

	public RandomLocations(double[] min, double[] max) {
		this.min = min;
		this.max = max;
	}

	public Location next(int index) {
		Coordinate coordinate = Coordinate.newInstance(randomBetween(min[0], max[0]), randomBetween(min[1], max[1]));
		return Location.Builder.newInstance().setId("" + index).setIndex(index).setCoordinate(coordinate).build();
	}

	public double randomBetween(double min, double max) {
		return min + (max - min) * r.nextDouble();
	}

}

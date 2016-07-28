package iot.project;

import java.awt.geom.Point2D;

import org.slf4j.LoggerFactory;

import com.graphhopper.PathWrapper;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iot.project.distanceCalculation.GraphHopperHelper;

public class Passenger {

	private int id;
	private Point2D.Double startingPointLatLong;
	private double revenue;

	public Passenger(int _id) {
		id = _id;
	}

	public Passenger(int _id, Point2D.Double _start) {
		id = _id;
		startingPointLatLong = _start;
	}

	public void calculateRevenue(Point2D.Double endPoint, GraphHopperHelper helper) {
		Logger log = LoggerFactory.getLogger(Main.class);
		PathWrapper bestPath;
		double distance = -1;
		if (helper != null) {
			try {
				bestPath = helper.route(startingPointLatLong.x, startingPointLatLong.y, endPoint.x, endPoint.y);

				distance = bestPath.getDistance();
				long timeInMs = bestPath.getTime();
				System.out.println(distance);
				
				log.info("DISTANCE = " + distance);
			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (distance == -1) {
			// just use default, fixed amount of money
			revenue= 3.75;
		} else {
			if (distance == 0) {
				revenue= 0.5*(1+(int)(Math.random()*10));
			}else {
				revenue= distance*0.75;	
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		Passenger p = (Passenger) o;
		if (p.id == this.id) {
			return true;
		}
		return false;
	}

	public int getId() {
		return id;
	}

	public double getRevenue() {
		return revenue;
	}
}

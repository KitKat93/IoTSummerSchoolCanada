package iot.project.distanceCalculation;

import java.io.File;

import org.kohsuke.args4j.Option;

public class CommandLineOptions {

	@Option(name = "--osm-file", usage = "OSM file to load", required = true)
	public File osmFile = null;

	@Option(name = "-h", aliases = { "--help" }, usage = "This help message.", required = false)
	public boolean help = false;

	@Option(name = "-v", aliases = { "--verbose" }, usage = "Verbose (DEBUG) logging output (default: INFO).", required = false)
	public boolean verbose = false;

}
/**
 * 
 */
package inat.analyser.uppaal;

import inat.InatBackend;
import inat.util.XmlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author B. Wanders
 */
public class UppaalInvoker {
	/**
	 * The configuration key for the leaveFiles property.
	 */
	private static final String LEAVE_KEY = "/Inat/UppaalInvoker/leaveFiles";

	/**
	 * The configuration key for the temporary location property.
	 */
	private static final String TEMP_KEY = "/Inat/UppaalInvoker/temporary";

	/**
	 * The configuration key for the tracer path property.
	 */
	private static final String TRACER_KEY = "/Inat/UppaalInvoker/tracer";

	/**
	 * The configuration key for the verifyta path property.
	 */
	private static final String VERIFY_KEY = "/Inat/UppaalInvoker/verifyta";

	/**
	 * Environment flag to switch UPPAAL into compile-only mode.
	 */
	private static final String UPPAAL_COMPILE_ONLY = "UPPAAL_COMPILE_ONLY";

	/**
	 * The verifier is used to compile the model and produce xtr files.
	 */
	private String verifytaPath;

	/**
	 * The tracer is used to combine a compiled model and an xtr file to
	 * generate a human-readable trace.
	 */
	private String tracerPath;

	/**
	 * The temporary location.
	 */
	private File temporaryLocation;

	/**
	 * Should we leave the temporary files?
	 */
	private boolean leaveFiles;

	/**
	 * Constructor.
	 */
	public UppaalInvoker() {
		assert InatBackend.isInitialised() : "Can only use the UppaalInvoker if the ANIMO backend is initialised.";

		// get configuration
		XmlConfiguration configuration = InatBackend.get().configuration();

		this.verifytaPath = configuration.get(VERIFY_KEY);
		this.tracerPath = configuration.get(TRACER_KEY);

		this.temporaryLocation = new File(configuration.get(TEMP_KEY, System.getProperty("java.io.tmpdir")));

		this.leaveFiles = configuration.has(LEAVE_KEY);
	}

	/**
	 * Constructor.
	 * 
	 * @param temporaryLocation the temporary file location
	 * @param tracerPath the path to the tracer executable
	 * @param verifytaPath the path to the verifyta executable
	 * @param leaveFiles should we leave the temporary files
	 */
	public UppaalInvoker(File temporaryLocation, String tracerPath, String verifytaPath, boolean leaveFiles) {
		this.temporaryLocation = temporaryLocation;
		this.tracerPath = tracerPath;
		this.verifytaPath = verifytaPath;
		this.leaveFiles = leaveFiles;
	}

	/**
	 * Produces a trace of the single query in the file. This method should only
	 * be used for a single query that results a trace.
	 * 
	 * @param model the UPPAAL model to use
	 * @param query the query to use
	 * @return a trace that satisfies the query, or {@code null} if the query
	 *         could not be satisfied
	 * @throws IOException if the trace could not be generated for some reason
	 * @throws InterruptedException if the trace process is interrupted for some
	 *             reason
	 */
	public String trace(String model, String query) throws IOException, InterruptedException {
		File modelFile = File.createTempFile("ANIMO", ".xml", this.temporaryLocation);
		final String prefix = modelFile.getName().replace(".xml", "");
		File queryFile = new File(this.temporaryLocation, prefix + ".q");

		// write out strings to file
		FileWriter modelFileOut = new FileWriter(modelFile);
		modelFileOut.append(model);
		modelFileOut.close();
		if (!this.leaveFiles) {
			modelFile.deleteOnExit();
		}

		FileWriter queryFileOut = new FileWriter(queryFile);
		queryFileOut.append(query);
		queryFileOut.close();
		if (!this.leaveFiles) {
			queryFile.deleteOnExit();
		}

		// create process builder
		ProcessBuilder builder = new ProcessBuilder();

		// step 1: create xtr
		builder.command(this.verifytaPath, "-t0", "-o2", "-y", "-f" + prefix, modelFile.getAbsolutePath(),
				queryFile.getAbsolutePath());
		builder.directory(this.temporaryLocation);
		builder.redirectErrorStream(true);
		Process xtrProcess = builder.start();

		BufferedReader reader = new BufferedReader(new InputStreamReader(xtrProcess.getInputStream()));

		StringBuilder buffer = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			buffer.append(line).append("\n");
		}

		int result = xtrProcess.waitFor();
		if (result != 0) {
			throw new IOException("Creating XTR's failed:\n" + buffer);
		}

		File xtrFile = new File(this.temporaryLocation, prefix + "-1.xtr");
		if (!xtrFile.exists()) {
			// no XTR file -> no satisfying trace (but the query might still be
			// answered in the positive, it just does not result in a trace)
			return null;
		}

		if (!this.leaveFiles) {
			xtrFile.deleteOnExit();
		}

		// step 2: compile model
		builder.command(this.verifytaPath, "-t0", "-o2", "-y", modelFile.getAbsolutePath(), queryFile.getAbsolutePath());
		builder.redirectErrorStream(true);
		builder.environment().put(UPPAAL_COMPILE_ONLY, "true");
		Process compilerProcess = builder.start();

		reader = new BufferedReader(new InputStreamReader(compilerProcess.getInputStream()));
		buffer = new StringBuilder();
		line = null;
		while ((line = reader.readLine()) != null) {
			buffer.append(line).append("\n");
		}

		File compiledFile = new File(this.temporaryLocation, prefix + ".model");
		if (!this.leaveFiles) {
			compiledFile.deleteOnExit();
		}

		result = compilerProcess.waitFor();
		if (result != 0) {
			throw new IOException("Compiling model failed:\n" + buffer);
		} else {
			FileWriter compiledFileOut = new FileWriter(compiledFile);
			compiledFileOut.append(buffer.toString());
			compiledFileOut.close();
		}

		// step 3: convert trace
		builder.command(this.tracerPath, compiledFile.getAbsolutePath(), xtrFile.getAbsolutePath());
		builder.redirectErrorStream(true);
		builder.environment().remove(UPPAAL_COMPILE_ONLY);

		Process tracerProcess = builder.start();

		reader = new BufferedReader(new InputStreamReader(tracerProcess.getInputStream()));
		buffer = new StringBuilder();
		line = null;
		while ((line = reader.readLine()) != null) {
			buffer.append(line).append("\n");
		}

		result = tracerProcess.waitFor();
		if (result != 0) {
			throw new IOException("Trace conversion failed because tracer process exitted with non-null value '"
					+ result + "'.\nOutput buffer so far was: " + buffer);
		} else {
			return buffer.toString();
		}
	}
}

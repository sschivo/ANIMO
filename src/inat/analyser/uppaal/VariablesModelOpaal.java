package inat.analyser.uppaal;

import inat.InatBackend;
import inat.model.Model;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.util.XmlConfiguration;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * This is the same as the reactant-centered model, but without using
 * structs or typedefs, as they are currently unsupported by opaal
 */
public class VariablesModelOpaal extends VariablesModelReactantCentered {

	public VariablesModelOpaal() {
		
	}


	private static final String REACTANT_INDEX = Model.Properties.REACTANT_INDEX,
								OUTPUT_REACTANT = Model.Properties.OUTPUT_REACTANT,
								SCENARIO = Model.Properties.SCENARIO,
								HAS_INFLUENCING_REACTIONS = "has influencing reactions";
	private boolean normalModelChecking = false;
	private int uncertainty = 0;
	
	@Override
	protected void appendModel(StringBuilder out, Model m) {
		out.append("<?xml version='1.0' encoding='utf-8'?>");
		out.append(newLine);
		out.append("<!DOCTYPE nta PUBLIC '-//Uppaal Team//DTD Flat System 1.1//EN' 'http://www.it.uu.se/research/group/darts/uppaal/flat-1_1.dtd'>");
		out.append(newLine);
		out.append("<nta>");
		out.append(newLine);
		out.append("<declaration>");
		out.append(newLine);
		
		// output global declarations
		out.append("// Place global declarations here.");
		out.append(newLine);
		out.append("clock globalTime;");
		out.append(newLine);
		out.append("const int INFINITE_TIME = " + INFINITE_TIME + ";");
		out.append(newLine);
		int countReactants = 0;
		for (Reactant r : m.getReactantCollection()) {
			if (r.get(ENABLED).as(Boolean.class)) {
				countReactants++;
			}
		}
		out.append("const int N_REACTANTS = " + countReactants + ";");
		out.append(newLine);
		out.append("broadcast chan reacting[N_REACTANTS];");
		out.append(newLine);
		if (m.getProperties().get(Model.Properties.MODEL_CHECKING_TYPE) != null && m.getProperties().get(Model.Properties.MODEL_CHECKING_TYPE).as(Integer.class) == Model.Properties.NORMAL_MODEL_CHECKING) {
			normalModelChecking = true;
			out.append("broadcast chan sequencer[N_REACTANTS];");
			out.append(newLine);
			if (countReactants > 1) {
				out.append("chan priority ");
				for (int i=0;i<countReactants-1;i++) {
					out.append("reacting[" + i + "] &lt; ");
				}
				out.append("reacting[" + (countReactants-1) + "];");
				out.append(newLine);
				out.append("chan priority ");
				for (int i=0;i<countReactants-1;i++) {
					out.append("sequencer[" + i + "] &lt; ");
				}
				out.append("sequencer[" + (countReactants-1) + "];");
				out.append(newLine);
			}
		} else {
			normalModelChecking = false;
		}
		out.append(newLine);
		
		int reactantIndex = 0;
		for (Reactant r : m.getReactantCollection()) {
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			r.let(REACTANT_INDEX).be(reactantIndex);
			reactantIndex++; 
			this.appendReactantVariables(out, r);
		}
		
		
		//The encoding of double numbers with integer variables (exponential notation with 3 significant figures)
		out.append(newLine);
//		out.append("typedef struct {");
//		out.append(newLine);
//		out.append("\tint[-99980001, 99980001] b;"); //99980001 = 9999 * 9999, i.e. the maximum result of a multiplication between two .b of double numbers
//		out.append(newLine);
//		out.append("\tint e;");
//		out.append(newLine);
//		out.append("} double;");
//		out.append(newLine);
		out.append(newLine);
		out.append("const int[-99980001, 99980001] zero_b = 0;");
		out.append(newLine);
		out.append("const int zero_e = 0;");
		out.append(newLine);
		out.append("const int[-99980001, 99980001] INFINITE_TIME_DOUBLE_b = -1000; //INFINITE_TIME (-1) translated into double");
		out.append(newLine);
		out.append("const int INFINITE_TIME_DOUBLE_e = -3;");
		out.append(newLine);
		uncertainty = 0;
		XmlConfiguration configuration = InatBackend.get().configuration();
		String uncertaintyStr = configuration.get(XmlConfiguration.UNCERTAINTY_KEY, null);
		if (uncertaintyStr != null) {
			try {
				uncertainty = Integer.parseInt(uncertaintyStr);
			} catch (Exception ex) {
				uncertainty = 0;
			}
		} else {
			uncertainty = 0;
		}
		if (uncertainty != 0) {
			double lowerUnc = (100.0 - uncertainty) / 100.0,
				   upperUnc = (100.0 + uncertainty) / 100.0;
			out.append("//Lower and upper scale factors to apply uncertainty. E.g. for +/- 5% uncertainty, we have lower uncertainty = 0.95, upper uncertainty = 1.05");
			out.append(newLine);
			formatDoubleDeclaration("const ", out, "LOWER_UNC", lowerUnc);
			formatDoubleDeclaration("const ", out, "UPPER_UNC", upperUnc);
		}
		out.append(newLine);
//		out.append("typedef int[-1, 1073741822] time_t;"); //The type for time values
//		out.append(newLine);
//		out.append(newLine);
//		//In order to still show the reaction activity ratio we use the original time bounds
//		out.append("typedef struct {");
//		out.append(newLine);
//		out.append("\ttime_t T;");
//		out.append(newLine);
//		out.append("} timeActivity;");
//		out.append(newLine);
//		out.append(newLine);
		
		for (Reaction r : m.getReactionCollection()) { //The time tables (filled by the rates, not times)
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			this.appendReactionTables(out, m, r);
		}
		out.append(newLine);
		out.append(newLine);
		out.append("//return value for all functions (int is used to be able to return before the end of the function, as return; in a void function doesn't seem to work)");
		out.append(newLine);
		out.append("int[-99980001, 99980001] retval_b;");
		out.append(newLine);
		out.append("int retval_e;");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("//parameters for double-related functions");
		out.append(newLine);
		out.append("int[-99980001, 99980001] a_b, b_b;");
		out.append(newLine);
		out.append("int a_e, b_e;");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("//parameters for scenario functions");
		out.append(newLine);
		out.append("int[-99980001, 99980001] k_b, r2_b, r1_b, r2Levels_b, r1Levels_b;");
		out.append(newLine);
		out.append("int k_e, r2_e, r1_e, r2Levels_e, r1Levels_e;");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("int subtract() { // a - b");
		out.append(newLine);
		out.append("\tretval_b = -1000;");
		out.append(newLine);
		out.append("\tretval_e = -1000;");
		out.append(newLine);
		out.append("\tif (a_b == 0) {");
		out.append(newLine);
		out.append("\t\tretval_b = -b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = b_e;");
		out.append(newLine);
		out.append("\t\treturn 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b_b == 0) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t\treturn 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((a_e - b_e) &gt;= 4) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t\treturn 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((b_e - a_e) &gt;= 4) {");
		out.append(newLine);
		out.append("\t\tretval_b = -b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = b_e;");
		out.append(newLine);
		out.append("\t\treturn 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e == b_e) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b - b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e - b_e == 1) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b - b_b/10;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e - b_e == 2) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b - b_b/100;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e - b_e == 3) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b - b_b/1000;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b_e - a_e == 1) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b/10 - b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = b_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b_e - a_e == 2) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b/100 - b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = b_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b_e - a_e == 3) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b/1000 - b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = b_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 10) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 1000;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 3;");
		out.append(newLine);
		out.append("\t} else if ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 100) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 100;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 2;");
		out.append(newLine);
		out.append("\t} else if ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 1000) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 10;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 1;");
		out.append(newLine);
		out.append("\t} else if (retval_b &gt; 9999 || retval_b &lt; -9999) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b / 10;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e + 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn 0;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("int add() { // a + b");
		out.append(newLine);
		out.append("\tretval_b = -1000;");
		out.append(newLine);
		out.append("\tretval_e = -1000;");
		out.append(newLine);
		out.append("\tif (a_b == 0) {");
		out.append(newLine);
		out.append("\t\tretval_b = b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = b_e;");
		out.append(newLine);
		out.append("\t\treturn 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b_b == 0) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t\treturn 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((a_e - b_e) &gt;= 4) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t\treturn 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((b_e - a_e) &gt;= 4) {");
		out.append(newLine);
		out.append("\t\tretval_b = b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = b_e;");
		out.append(newLine);
		out.append("\t\treturn 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e == b_e) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b + b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e - b_e == 1) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b + b_b/10;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e - b_e == 2) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b + b_b/100;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e - b_e == 3) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b + b_b/1000;");
		out.append(newLine);
		out.append("\t\tretval_e = a_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b_e - a_e == 1) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b/10 + b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = b_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b_e - a_e == 2) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b/100 + b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = b_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b_e - a_e == 3) {");
		out.append(newLine);
		out.append("\t\tretval_b = a_b/1000 + b_b;");
		out.append(newLine);
		out.append("\t\tretval_e = b_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 10) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 1000;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 3;");
		out.append(newLine);
		out.append("\t} else if ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 100) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 100;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 2;");
		out.append(newLine);
		out.append("\t} else if ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 1000) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 10;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 1;");
		out.append(newLine);
		out.append("\t} else if (retval_b &gt; 9999 || retval_b &lt; -9999) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b / 10;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e + 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn 0;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("int multiply() { // a * b");
		out.append(newLine);
		out.append("\tretval_b = a_b * b_b;");
		out.append(newLine);
		out.append("\tif (retval_b % 1000 &lt; 500) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b / 1000;");
		out.append(newLine);
		out.append("\t} else {");
		out.append(newLine);
		out.append("\t\tretval_b = 1 + retval_b / 1000;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tretval_e = a_e + b_e + 3;");
		out.append(newLine);
		out.append("\tif ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 10) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 1000;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 3;");
		out.append(newLine);
		out.append("\t} else if ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 100) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 100;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 2;");
		out.append(newLine);
		out.append("\t} else if ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 1000) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 10;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 1;");
		out.append(newLine);
		out.append("\t} else if (retval_b &gt; 9999 || retval_b &lt; -9999) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b / 10;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e + 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn 0;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("int inverse() { // 1 / a");
		out.append(newLine);
		out.append("\tif (a_b == 0) {");
		out.append(newLine);
		out.append("\t\tretval_b = INFINITE_TIME_DOUBLE_b;");
		out.append(newLine);
		out.append("\t\tretval_e = INFINITE_TIME_DOUBLE_e;");
		out.append(newLine);
		out.append("\t\treturn 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tretval_b = 1000000 / a_b;");
		out.append(newLine);
		out.append("\tretval_e = -6 - a_e;");
		out.append(newLine);
		out.append("\tif ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 10) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 1000;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 3;");
		out.append(newLine);
		out.append("\t} else if ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 100) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 100;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 2;");
		out.append(newLine);
		out.append("\t} else if ((retval_b &gt; 0 &amp;&amp; retval_b &lt; 1000) || (retval_b &lt; 0 &amp;&amp; retval_b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tretval_b = retval_b * 10;");
		out.append(newLine);
		out.append("\t\tretval_e = retval_e - 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn 0;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("int[-1, 1073741822] power(int base, int exponent) { // base ^ exponent (exponent &gt;= 0)");
		out.append(newLine);
		out.append("\tint[-1, 1073741822] r = 1;");
		out.append(newLine);
		out.append("\twhile (exponent &gt; 0) {");
		out.append(newLine);
		out.append("\t\tr = r * base;");
		out.append(newLine);
		out.append("\t\texponent = exponent - 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("int[-1, 1073741822] round() { // a double --&gt; integer");
		out.append(newLine);
		out.append("\tif (a_e &lt; -3) {");
		out.append(newLine);
		out.append("\t\tif (a_b &lt; 5000) return 0;");
		out.append(newLine);
		out.append("\t\telse return 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e == -1) {");
		out.append(newLine);
		out.append("\t\tif (a_b % 10 &lt; 5) {");
		out.append(newLine);
		out.append("\t\t\treturn a_b / 10;");
		out.append(newLine);
		out.append("\t\t} else {");
		out.append(newLine);
		out.append("\t\t\treturn 1 + a_b / 10;");
		out.append(newLine);
		out.append("\t\t}");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e == -2) {");
		out.append(newLine);
		out.append("\t\tif (a_b % 100 &lt; 50) {");
		out.append(newLine);
		out.append("\t\t\treturn a_b / 100;");
		out.append(newLine);
		out.append("\t\t} else {");
		out.append(newLine);
		out.append("\t\t\treturn 1 + a_b / 100;");
		out.append(newLine);
		out.append("\t\t}");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a_e == -3) {");
		out.append(newLine);
		out.append("\t\tif (a_b % 1000 &lt; 500) {");
		out.append(newLine);
		out.append("\t\t\treturn a_b / 1000;");
		out.append(newLine);
		out.append("\t\t} else {");
		out.append(newLine);
		out.append("\t\t\treturn 1 + a_b / 1000;");
		out.append(newLine);
		out.append("\t\t}");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn a_b * power(10, a_e);");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("int scenario1(bool r1Active) {");
		out.append(newLine);
		out.append("\tint[-99980001, 99980001] E_b;");
		out.append(newLine);
		out.append("\tint E_e;");
		out.append(newLine);
		out.append("\tif (r1Active) { //If we depend on active R1, the level of activity is the value of E");
		out.append(newLine);
		out.append("\t\tE_b = r1_b;");
		out.append(newLine);
		out.append("\t\tE_e = r1_e;");
		out.append(newLine);
		out.append("\t} else { //otherwise we find the inactivity level via the total number of levels");
		out.append(newLine);
		out.append("\t\ta_b = r1Levels_b;");
		out.append(newLine);
		out.append("\t\ta_e = r1Levels_e;");
		out.append(newLine);
		out.append("\t\tb_b = r1_b;");
		out.append(newLine);
		out.append("\t\tb_e = r1_e;");
		out.append(newLine);
		out.append("\t\tsubtract();");
		out.append(newLine);
		out.append("\t\tE_b = retval_b;");
		out.append(newLine);
		out.append("\t\tE_e = retval_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\ta_b = k_b;");
		out.append(newLine);
		out.append("\ta_e = k_e;");
		out.append(newLine);
		out.append("\tb_b = E_b;");
		out.append(newLine);
		out.append("\tb_e = E_e;");
		out.append(newLine);
		out.append("\tmultiply();");
		out.append(newLine);
		out.append("\treturn 0;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("int scenario2_3(bool r2Active, bool r1Active) {");
		out.append(newLine);
		out.append("\tint[-99980001, 99980001] E_b, S_b, ret1_b;");
		out.append(newLine);
		out.append("\tint E_e, S_e, ret1_e;");
		out.append(newLine);
		out.append("\tif (r1Active) { //If we depend on active R1, the level of activity is the value of E");
		out.append(newLine);
		out.append("\t\tE_b = r1_b;");
		out.append(newLine);
		out.append("\t\tE_e = r1_e;");
		out.append(newLine);
		out.append("\t} else { //otherwise we find the inactivity level via the total number of levels");
		out.append(newLine);
		out.append("\t\ta_b = r1Levels_b;");
		out.append(newLine);
		out.append("\t\ta_e = r1Levels_e;");
		out.append(newLine);
		out.append("\t\tb_b = r1_b;");
		out.append(newLine);
		out.append("\t\tb_e = r1_e;");
		out.append(newLine);
		out.append("\t\tsubtract();");
		out.append(newLine);
		out.append("\t\tE_b = retval_b;");
		out.append(newLine);
		out.append("\t\tE_e = retval_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (r2Active) { //Same for R2");
		out.append(newLine);
		out.append("\t\tS_b = r2_b;");
		out.append(newLine);
		out.append("\t\tS_e = r2_e;");
		out.append(newLine);
		out.append("\t} else {");
		out.append(newLine);
		out.append("\t\ta_b = r2Levels_b;");
		out.append(newLine);
		out.append("\t\ta_e = r2Levels_e;");
		out.append(newLine);
		out.append("\t\tb_b = r2_b;");
		out.append(newLine);
		out.append("\t\tb_e = r2_e;");
		out.append(newLine);
		out.append("\t\tsubtract();");
		out.append(newLine);
		out.append("\t\tS_b = retval_b;");
		out.append(newLine);
		out.append("\t\tS_e = retval_e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\ta_b = E_b;");
		out.append(newLine);
		out.append("\ta_e = E_e;");
		out.append(newLine);
		out.append("\tb_b = S_b;");
		out.append(newLine);
		out.append("\tb_e = S_e;");
		out.append(newLine);
		out.append("\tmultiply();");
		out.append(newLine);
		out.append("\tret1_b = retval_b;");
		out.append(newLine);
		out.append("\tret1_e = retval_e;");
		out.append(newLine);
		out.append("\ta_b = k_b;");
		out.append(newLine);
		out.append("\ta_e = k_e;");
		out.append(newLine);
		out.append("\tb_b = ret1_b;");
		out.append(newLine);
		out.append("\tb_e = ret1_e;");
		out.append(newLine);
		out.append("\tmultiply();");
		out.append(newLine);
		out.append("\treturn 0;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("int int_to_double(int n) { //Used to translate an activity level into double.");
		out.append(newLine);
		out.append("\tif (n &lt; 10) {");
		out.append(newLine);
		out.append("\t\tretval_b = n * 1000;");
		out.append(newLine);
		out.append("\t\tretval_e = -3;");
		out.append(newLine);
		out.append("\t} else if (n &lt; 100) {");
		out.append(newLine);
		out.append("\t\tretval_b = n * 100;");
		out.append(newLine);
		out.append("\t\tretval_e = -2;");
		out.append(newLine);
		out.append("\t} else if (n &lt; 1000) {");
		out.append(newLine);
		out.append("\t\tretval_b = n * 10;");
		out.append(newLine);
		out.append("\t\tretval_e = -1;");
		out.append(newLine);
		out.append("\t} else if (n &lt; 10000) { //Our model supports up to 100 levels, so this should be the most it makes sense to check");
		out.append(newLine);
		out.append("\t\tretval_b = n;");
		out.append(newLine);
		out.append("\t\tretval_e = 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn 0;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		
		out.append("</declaration>");
		
		out.append(newLine);
		out.append(newLine);
		// output templates
		this.appendTemplates(out, m);
		
		out.append(newLine);
		out.append("<system>");
		out.append(newLine);
		
//		for (Reactant r : m.getReactantCollection()) {
//			if (!r.get(ENABLED).as(Boolean.class) || !r.get(HAS_INFLUENCING_REACTIONS).as(Boolean.class)) continue;
//			this.appendReactionProcess(out, m, r, reactantIndex);
//		}
//		
//		out.append(newLine);
//		out.append(newLine);
		out.append(newLine);
		
		// compose the system
		out.append("system ");
		Iterator<Reactant> iter = m.getReactantCollection().iterator();
		boolean first = true;
		while (iter.hasNext()) {
			Reactant r = iter.next();
			if (!r.get(ENABLED).as(Boolean.class) || !r.get(HAS_INFLUENCING_REACTIONS).as(Boolean.class)) continue;
			if (!first) {
				out.append(", ");
			}
			out.append("_" + r.getId());
			first = false;
		}
		out.append(";");
		
		out.append(newLine);
		out.append(newLine);
		out.append("</system>");
		out.append(newLine);
		out.append("</nta>");
	}
	
//	private void appendReactionProcess(StringBuilder out, Model m, Reactant r, int index) {
//		out.append(r.getId() + "_ = _" + r.getId() + "(" + r.getId() + ", " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + ");");
//		out.append(newLine);
//	}
	
	private void appendReactionTables(StringBuilder out, Model m, Reaction r) {
		String r1Id = r.get(CATALYST).as(String.class);
		String r2Id = r.get(REACTANT).as(String.class);
		String rOutput = r.get(Model.Properties.OUTPUT_REACTANT).as(String.class);
		out.append("//Reaction " + r1Id + " (" + m.getReactant(r1Id).get(ALIAS).as(String.class) + ") " + (!r2Id.equals(rOutput)?("AND " + r2Id + " (" + m.getReactant(r2Id).get(ALIAS).as(String.class) + ") "):"") + (r.get(INCREMENT).as(Integer.class)>0?"-->":"--|") + " " + (r2Id.equals(rOutput)?(r2Id + " (" + m.getReactant(r2Id).get(ALIAS).as(String.class) + ")"):(rOutput + " (" + m.getReactant(rOutput).get(ALIAS).as(String.class) + ")")));
		out.append(newLine);
		
		out.append("int[-1, 1073741822] " + r.getId() + "_T;");
		out.append(newLine);
//		out.append("const double k_" + r.getId() + " = " + ";");
		formatDoubleDeclaration(out, "k_" + r.getId(), r.get(Model.Properties.SCENARIO_PARAMETER_K).as(Double.class) / (m.getProperties().get(Model.Properties.SECS_POINT_SCALE_FACTOR).as(Double.class) * r.get(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").as(Double.class)));
		out.append(newLine);
		out.append(newLine);
		
	}


	@Override
	protected void appendTemplates(StringBuilder out, Model m) {
		try {
			StringWriter outString;
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document;
			Transformer tra = TransformerFactory.newInstance().newTransformer();
			tra.setOutputProperty(OutputKeys.INDENT, "yes");
			tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			for (Reactant r : m.getReactantCollection()) {
				if (!r.get(ENABLED).as(Boolean.class)) continue;
				outString = new StringWriter();
				Vector<Reaction> influencingReactions = new Vector<Reaction>();
				for (Reaction re : m.getReactionCollection()) {
					if (re.get(OUTPUT_REACTANT).as(String.class).equals(r.getId()))  { //If the reactant is downstream of a reaction, count that reaction
						influencingReactions.add(re);
					}
				}
				
				if (influencingReactions.size() < 1) {
					r.let(HAS_INFLUENCING_REACTIONS).be(false);
					continue;
				}
				r.let(HAS_INFLUENCING_REACTIONS).be(true);
				
				StringBuilder template = new StringBuilder("<template><name>_" + r.getId() + "</name><declaration>int[-1, 1] delta, deltaNew, deltaOld, deltaOldOld, deltaOldOldOld;\nbool deltaAlternating;\nint[-1, 1073741822] tL, tU;\nclock c;\nint[-99980001, 99980001] totalRate_b;\nint totalRate_e;\n\n\nvoid updateDeltaOld() {\n\tdeltaOldOldOld = deltaOldOld;\n\tdeltaOldOld = deltaOld;\n\tdeltaOld = deltaNew;\n\tdeltaNew = delta;\n\tdeltaAlternating = false;\n\tif (deltaOldOldOld != 0) { //We have updated delta at least 4 times, so we can see whether we have an oscillation\n\t\tif (deltaNew == deltaOldOld &amp;&amp; deltaOld == deltaOldOldOld &amp;&amp; deltaNew != deltaOld) { //Pairwise equal and alternating (e.g. +1, -1, +1, -1): we are oscillating\n\t\t\tdeltaAlternating = true;\n\t\t}\n\t}\n}\n\nvoid update() {\n");
				for (int i=0; i<influencingReactions.size(); i++) {
					formatDoubleDeclaration("\t", template, "ret" + i, 0);
				}
				int countReaction = 0;
				for (Reaction re : influencingReactions) {
					int scenario = re.get(SCENARIO).as(Integer.class);
					boolean activeR1, activeR2;
					if (scenario == 0 || scenario == 1) {
						activeR1 = true;
						if (re.get(INCREMENT).as(Integer.class) >= 0) {
							activeR2 = false;
						} else {
							activeR2 = true;
						}
					} else if (scenario == 2) {
						activeR1 = re.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1").as(Boolean.class);
						activeR2 = re.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2").as(Boolean.class);
					} else {
						//TODO: this should never happen, because we have already made these checks
						activeR1 = activeR2 = true;
					}
					template.append("\tk_b = k_" + re.getId() + "_b;");
					template.append(newLine);
					template.append("\tk_e = k_" + re.getId() + "_e;");
					template.append(newLine);
					switch (scenario) {
						case 0:
							template.append("\tint_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + ");");
							template.append(newLine);
							template.append("\tr1_b = retval_b;");
							template.append(newLine);
							template.append("\tr1_e = retval_e;");
							template.append(newLine);
							template.append("\tr1Levels_b = " + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "Levels_b;");
							template.append(newLine);
							template.append("\tr1Levels_e = " + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "Levels_e;");
							template.append(newLine);
							template.append("\tscenario1(" + activeR1 + ");");
							template.append(newLine);
							break;
						case 1: case 2:
							template.append("\tint_to_double(" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + ");");
							template.append(newLine);
							template.append("\tr2_b = retval_b;");
							template.append(newLine);
							template.append("\tr2_e = retval_e;");
							template.append(newLine);
							template.append("\tr2Levels_b = " + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "Levels_b;");
							template.append(newLine);
							template.append("\tr2Levels_e = " + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "Levels_e;");
							template.append(newLine);
							template.append("\tint_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + ");");
							template.append(newLine);
							template.append("\tr1_b = retval_b;");
							template.append(newLine);
							template.append("\tr1_e = retval_e;");
							template.append(newLine);
							template.append("\tr1Levels_b = " + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "Levels_b;");
							template.append(newLine);
							template.append("\tr1Levels_e = " + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "Levels_e;");
							template.append(newLine);
							template.append("\tscenario2_3(" + activeR2 + ", " + activeR1 + ");");
							template.append(newLine);
							break;
						default:
							break;
					}
					template.append("\tret" + countReaction + "_b = retval_b;");
					template.append(newLine);
					template.append("\tret" + countReaction + "_e = retval_e;");
					template.append(newLine);
					template.append("\ta_b = ret" + countReaction + "_b;");
					template.append(newLine);
					template.append("\ta_e = ret" + countReaction + "_e;");
					template.append(newLine);
					template.append("\tinverse();");
					template.append(newLine);
					template.append("\ta_b = retval_b;");
					template.append(newLine);
					template.append("\ta_e = retval_e;");
					template.append(newLine);
					if (uncertainty != 0) {
						template.append("\tb_b = LOWER_UNC_b;");
						template.append(newLine);
						template.append("\tb_e = LOWER_UNC_e;");
						template.append(newLine);
						template.append("\tmultiply();");
						template.append(newLine);
						template.append("\ta_b = retval_b;");
						template.append(newLine);
						template.append("\ta_e = retval_e;");
						template.append(newLine);
					}
					template.append("\t" + re.getId() + "_T = round();");
					template.append(newLine);
					String nameForA = "ret" + (countReaction - 1);
					if (countReaction == 0) {
						nameForA = "zero";
					}
					template.append("\ta_b = " + nameForA + "_b;");
					template.append(newLine);
					template.append("\ta_e = " + nameForA + "_e;");
					template.append(newLine);
					template.append("\tb_b = ret" + countReaction + "_b;");
					template.append(newLine);
					template.append("\tb_e = ret" + countReaction + "_e;");
					template.append(newLine);
					if (re.get(Model.Properties.INCREMENT).as(Integer.class) > 0) {
						template.append("\tadd();");
					} else {
						template.append("\tsubtract();");
					}
					template.append(newLine);
					template.append("\tret" + countReaction + "_b = retval_b;");
					template.append(newLine);
					template.append("\tret" + countReaction + "_e = retval_e;");
					template.append(newLine);
					countReaction++;
				}
				template.append("\ttotalRate_b = ret" + (countReaction-1) + "_b;");
				template.append(newLine);
				template.append("\ttotalRate_e = ret" + (countReaction-1) + "_e;");
				template.append(newLine);
				
				template.append("\tif (totalRate_b &lt; 0) {\n\t\tdelta = -1;\n\t\ttotalRate_b = -totalRate_b;\n\t} else {\n\t\tdelta = 1;\n\t}\n\tif (totalRate_b != 0) {\n");
				template.append("\t\ta_b = totalRate_b;\n\t\ta_e = totalRate_e;\n\t\tinverse();\n");
				if (uncertainty != 0) {
					template.append("\t\ta_b = retval_b;\n\t\ta_e = retval_e;\n\t\tb_b = LOWER_UNC_b;\n\t\tb_e = LOWER_UNC_e;\n\t\tmultiply();\n");
				}
				template.append("\t\ta_b = retval_b;\n\t\ta_e = retval_e;\n\t\ttL = round();\n");
				if (uncertainty != 0) {
					template.append("\t\ta_b = retval_b;\n\t\ta_e = retval_e;\n\t\tb_b = UPPER_UNC_b;\n\t\tb_e = UPPER_UNC_e;\n\t\tmultiply();\n");
				}
				template.append("\t\ta_b = retval_b;\n\t\ta_e = retval_e;\n\t\ttU = round();\n");
				template.append("\t} else {\n\t\ttL = INFINITE_TIME;\n\t\ttU = INFINITE_TIME;\n\t}\n\tif (tL != INFINITE_TIME &amp;&amp; tL &gt; tU) { //We use rounded things: maybe the difference between tL and tU was not so great, and with some rounding problems we could have this case\n\t\ttL = tU;\n\t}\n\tupdateDeltaOld();\n}\n\nvoid react() {\n\tif (0 &lt;= " + r.getId() + " + delta &amp;&amp; " + r.getId() + " + delta &lt;= " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + ") {\n\t\t" + r.getId() + " = " + r.getId() + " + delta;\n\t}\n\tupdate();\n}\n\nbool can_react() {\n\treturn !deltaAlternating &amp;&amp; (tL != INFINITE_TIME &amp;&amp; tL != 0 &amp;&amp; tU != 0 &amp;&amp; ((delta &gt; 0 &amp;&amp; " + r.getId() + " &lt; " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + ") || (delta &lt; 0 &amp;&amp; " + r.getId() + " &gt; 0)));\n}\n\nbool cant_react() {\n\treturn deltaAlternating || (tL == INFINITE_TIME || tL == 0 || tU == 0 || (delta &gt; 0 &amp;&amp; " + r.getId() + " == " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + ") || (delta &lt; 0 &amp;&amp; " + r.getId() + " == 0));\n}</declaration>");
				template.append("<location id=\"id0\" x=\"-1896\" y=\"-728\"><name x=\"-1960\" y=\"-752\">stubborn</name><committed/></location><location id=\"id1\" x=\"-1528\" y=\"-728\"><committed/></location><location id=\"id6\" x=\"-1256\" y=\"-728\"><name x=\"-1248\" y=\"-752\">start</name><committed/></location><location id=\"id7\" x=\"-1552\" y=\"-856\"><name x=\"-1656\" y=\"-872\">not_reacting</name></location><location id=\"id8\" x=\"-1416\" y=\"-728\"><name x=\"-1400\" y=\"-752\">updating</name><committed/></location><location id=\"id9\" x=\"-1664\" y=\"-728\"><name x=\"-1728\" y=\"-744\">waiting</name><label kind=\"invariant\" x=\"-1728\" y=\"-720\">c &lt;= tU\n|| tU ==\nINFINITE_TIME</label></location><init ref=\"id6\"/><transition><source ref=\"id1\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1640\" y=\"-760\">tU == INFINITE_TIME\n|| c &lt;= tU</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1640\" y=\"-776\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "</transition><transition><source ref=\"id1\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1608\" y=\"-712\">tU != INFINITE_TIME\n&amp;&amp; c &gt; tU</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1608\" y=\"-664\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<label kind=\"assignment\" x=\"-1608\" y=\"-680\">c := tU</label><nail x=\"-1528\" y=\"-680\"/><nail x=\"-1608\" y=\"-680\"/></transition><transition><source ref=\"id0\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1816\" y=\"-632\">c &lt; tL</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1816\" y=\"-600\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<label kind=\"assignment\" x=\"-1816\" y=\"-616\">update()</label><nail x=\"-1848\" y=\"-616\"/><nail x=\"-1464\" y=\"-616\"/></transition><transition><source ref=\"id0\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1816\" y=\"-680\">c &gt;= tL</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1816\" y=\"-664\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<nail x=\"-1840\" y=\"-664\"/><nail x=\"-1744\" y=\"-664\"/></transition><transition><source ref=\"id6\"/><target ref=\"id8\"/>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1344\" y=\"-744\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<label kind=\"assignment\" x=\"-1344\" y=\"-728\">update()</label></transition>");
				int y1 = -904,
					y2 = -888,
					y3 = -848,
					incrY = -40;
				Vector<Reactant> alreadyOutputReactants = new Vector<Reactant>(); //Keep track of reactants that already have a transition to avoid input nondeterminism
				for (Reaction re : influencingReactions) { //Transitions from not_reacting to updating
					int scenario = re.get(SCENARIO).as(Integer.class);
					Reactant catalyst = m.getReactant(re.get(CATALYST).as(String.class)),
							 reactant = m.getReactant(re.get(REACTANT).as(String.class)); //This is not null only when scenario != 0
					switch (scenario) {
						case 0:
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update(), c:= 0</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						case 1:
						case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update(), c:= 0</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							if (reactant.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(reactant)) {
								alreadyOutputReactants.add(reactant);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(REACTANT).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update(), c:= 0</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						default:
							break;
					}
				}
				template.append("<transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1512\" y=\"-840\">cant_react()</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1512\" y=\"-856\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<nail x=\"-1416\" y=\"-824\"/><nail x=\"-1552\" y=\"-824\"/></transition><transition><source ref=\"id8\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1512\" y=\"-744\">can_react()</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1512\" y=\"-728\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "</transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1576\" y=\"-816\">c &gt;= tL</label><label kind=\"synchronisation\" x=\"-1584\" y=\"-800\">reacting[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label><label kind=\"assignment\" x=\"-1568\" y=\"-784\">react(), c := 0</label><nail x=\"-1632\" y=\"-784\"/><nail x=\"-1464\" y=\"-784\"/></transition>");
				y1 = -744;
				y2 = -728;
				incrY = -48;
				alreadyOutputReactants = new Vector<Reactant>(); //Keep trace of which reactants already have a transition for them, because otherwise we get input nondeterminism
				for (Reaction re : influencingReactions) { //Transitions from waiting to stubborn
					int scenario = re.get(SCENARIO).as(Integer.class);
					Reactant catalyst = m.getReactant(re.get(CATALYST).as(String.class)),
							 reactant = m.getReactant(re.get(REACTANT).as(String.class)); //This is not null only when scenario != 0
					switch (scenario) {
						case 0:
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id9\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1832\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><nail x=\"-1752\" y=\"" + y2 + "\"/><nail x=\"-1840\" y=\"" + y2 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						case 1:
						case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id9\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1832\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><nail x=\"-1752\" y=\"" + y2 + "\"/><nail x=\"-1840\" y=\"" + y2 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							if (reactant.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(reactant)) {
								alreadyOutputReactants.add(reactant);
								template.append("<transition><source ref=\"id9\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1832\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(REACTANT).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><nail x=\"-1752\" y=\"" + y2 + "\"/><nail x=\"-1840\" y=\"" + y2 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						default:
							break;
					}
				}
				template.append("</template>");
				document = documentBuilder.parse(new ByteArrayInputStream(template.toString().getBytes()));
				tra.transform(new DOMSource(document), new StreamResult(outString));
				out.append(outString.toString());
				out.append(newLine);
				out.append(newLine);
			}
		} catch (Exception e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
		}
	}
	
	protected String getReactionName(Reaction r) {
		return r.getId(); //The (UPPAAL) ID of the reaction is already set when we create it in the model
	}
	
	@Override
	protected void appendReactantVariables(StringBuilder out, Reactant r) {
		// outputs the global variables necessary for the given reactant
		out.append("//" + r.getId() + " = " + r.get(ALIAS).as(String.class));
		out.append(newLine);
		out.append("int " + r.getId() + " := " + r.get(INITIAL_LEVEL).as(Integer.class) + ";");
		out.append(newLine);
		formatDoubleDeclaration(out, r.getId() + "Levels", r.get(NUMBER_OF_LEVELS).as(Integer.class));
		out.append(newLine);
	}
	
	@Override
	protected String formatTime(int time) {
		if (time == INFINITE_TIME) {
			return "{0, 0}";
		} else {
			return formatDouble(1.0 / time); //time is guaranteed not to be 0, because we use 0 as a signal that rounding is not good enough, and increase time scale
		}
	}
	
	private String formatDouble(double d) {
		int b, e; 
		e = (int)Math.round(Math.log10(d)) - 3;
		b = (int)Math.round(d * Math.pow(10, -e));
		if (b < 10) { //We always want 4 figures
			b = b * 1000;
			e = e - 3;
		} else if (b < 100) {
			b = b * 100;
			e = e - 2;
		} else if (b < 1000) {
			b = b * 10;
			e = e - 1;
		}
		return "{" + b + ", " + e + "}";
	}
	
	private void formatDoubleDeclaration(StringBuilder out, String varName, double d) {
		formatDoubleDeclaration("", out, varName, d);
	}
	
	private void formatDoubleDeclaration(String prefix, StringBuilder out, String varName, double d) {
		int b, e; 
		e = (int)Math.round(Math.log10(d)) - 3;
		b = (int)Math.round(d * Math.pow(10, -e));
		if (b < 10) { //We always want 4 figures
			b = b * 1000;
			e = e - 3;
		} else if (b < 100) {
			b = b * 100;
			e = e - 2;
		} else if (b < 1000) {
			b = b * 10;
			e = e - 1;
		}
		out.append(prefix + "int[-99980001, 99980001] " + varName + "_b = " + b + ";");
		out.append(newLine);
		out.append(prefix + "int " + varName + "_e = " + e + ";");
		out.append(newLine);
	}

	
}

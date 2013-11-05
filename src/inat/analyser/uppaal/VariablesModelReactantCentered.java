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


public class VariablesModelReactantCentered extends VariablesModel {

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
		for (Reactant r : m.getSortedReactantList()) {
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			r.let(REACTANT_INDEX).be(reactantIndex);
			reactantIndex++; 
			this.appendReactantVariables(out, r);
		}
		
		
		//The encoding of double numbers with integer variables (exponential notation with 3 significant figures)
		out.append(newLine);
		out.append("typedef struct {");
		out.append(newLine);
		out.append("\tint[-99980001, 99980001] b;"); //99980001 = 9999 * 9999, i.e. the maximum result of a multiplication between two .b of double numbers
		out.append(newLine);
		out.append("\tint e;");
		out.append(newLine);
		out.append("} double_t;");
		out.append(newLine);
		out.append(newLine);
		out.append("const double_t zero = {0, 0};");
		out.append(newLine);
		out.append("const double_t INFINITE_TIME_DOUBLE = {-1000, -3}; //INFINITE_TIME (-1) translated into double");
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
			out.append("const double_t LOWER_UNC = " + formatDouble(lowerUnc) + ", //Lower and upper scale factors to apply uncertainty. E.g. for +/- 5% uncertainty, we have lower uncertainty = 0.95, upper uncertainty = 1.05");
			out.append(newLine);
			out.append("             UPPER_UNC = " + formatDouble(upperUnc) + ";");
			out.append(newLine);
		}
		out.append(newLine);
		out.append("typedef int[-1, 1073741822] time_t;"); //The type for time values
		out.append(newLine);
		out.append(newLine);
		//In order to still show the reaction activity ratio we use the original time bounds
		out.append("typedef struct {");
		out.append(newLine);
		out.append("\ttime_t T;");
		out.append(newLine);
		out.append("} timeActivity;");
		out.append(newLine);
		out.append(newLine);
		
		for (Reaction r : m.getReactionCollection()) { //The time tables (filled by the rates, not times)
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			this.appendReactionTables(out, m, r);
		}
		out.append(newLine);
		out.append(newLine);
		if (uncertainty == 0) {
			out.append("time_t T[N_REACTANTS] = {");
			for (int i = 0; i < countReactants - 1; i++) {
				out.append("0, ");
			}
			out.append("0};");
			out.append(newLine);
			out.append("clock c[N_REACTANTS];");
			out.append(newLine);
			out.append("bool active[N_REACTANTS] = {");
			for (int i = 0; i < countReactants - 1; i++) {
				out.append("false, ");
			}
			out.append("false};");
			out.append(newLine);
			out.append(newLine);
		}
		out.append("double_t subtract(double_t a, double_t b) { // a - b"); // Subtraction
		out.append(newLine);
		out.append("\tdouble_t r = {-1000, -1000};");
		out.append(newLine);
		out.append("\tif (a.b == 0) {");
		out.append(newLine);
		out.append("\t\tr.b = -b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t\treturn r;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.b == 0) {");
		out.append(newLine);
		out.append("\t\treturn a;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((a.e - b.e) &gt;= 4) return a;");
		out.append(newLine);
		out.append("\tif ((b.e - a.e) &gt;= 4) {");
		out.append(newLine);
		out.append("\t\tr.b = -b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t\treturn r;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e == b.e) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b - b.b;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 1) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b - b.b/10;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 2) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b - b.b/100;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 3) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b - b.b/1000;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 1) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/10 - b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 2) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/100 - b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 3) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/1000 - b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) || (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 1000;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 3;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) || (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 1000) || (r.b &lt; 0 &amp;&amp; r.b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 10;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 1;");
		out.append(newLine);
		out.append("\t} else if (r.b &gt; 9999 || r.b &lt; -9999) {");
		out.append(newLine);
        out.append("\t\tr.b = r.b / 10;");
		out.append(newLine);
        out.append("\t\tr.e = r.e + 1;");
		out.append(newLine);
    	out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double_t add(double_t a, double_t b) { // a + b"); // Addition
		out.append(newLine);
		out.append("\tdouble_t r = {-1000,-1000};");
		out.append(newLine);
		out.append("\tif (a.b == 0) {");
		out.append(newLine);
		out.append("\t\treturn b;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.b == 0) {");
		out.append(newLine);
		out.append("\t\treturn a;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((a.e - b.e) &gt;= 4) return a;");
		out.append(newLine);
		out.append("\tif ((b.e - a.e) &gt;= 4) return b;");
		out.append(newLine);
		out.append("\tif (a.e == b.e) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b + b.b;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 1) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b + b.b/10;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 2) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b + b.b/100;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 3) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b + b.b/1000;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 1) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/10 + b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 2) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/100 + b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 3) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/1000 + b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) || (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 1000;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 3;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) || (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 1000) || (r.b &lt; 0 &amp;&amp; r.b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 10;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 1;");
		out.append(newLine);
		out.append("\t} else if (r.b &gt; 9999 || r.b &lt; -9999) {");
		out.append(newLine);
        out.append("\t\tr.b = r.b / 10;");
		out.append(newLine);
        out.append("\t\tr.e = r.e + 1;");
		out.append(newLine);
    	out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double_t multiply(double_t a, double_t b) { // a * b"); // Multiplication
		out.append(newLine);
		out.append("\tdouble_t r;");
		out.append(newLine);
		out.append("\tr.b = a.b * b.b;");
		out.append(newLine);
		out.append("\tif (r.b % 1000 &lt; 500) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b / 1000;");
		out.append(newLine);
		out.append("\t} else {");
		out.append(newLine);
		out.append("\t\tr.b = 1 + r.b / 1000;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tr.e = a.e + b.e + 3;");
		out.append(newLine);
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) || (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 1000;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 3;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) || (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 1000) || (r.b &lt; 0 &amp;&amp; r.b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 10;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 1;");
		out.append(newLine);
		out.append("\t} else if (r.b &gt; 9999 || r.b &lt; -9999) {");
		out.append(newLine);
        out.append("\t\tr.b = r.b / 10;");
		out.append(newLine);
        out.append("\t\tr.e = r.e + 1;");
		out.append(newLine);
    	out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double_t inverse(double_t a) { // 1 / a"); // Inverse
		out.append(newLine);
		out.append("\tdouble_t r;");
		out.append(newLine);
		out.append("\tif (a.b == 0) {");
		out.append(newLine);
		out.append("\t\treturn INFINITE_TIME_DOUBLE;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tr.b = 1000000 / a.b;");
		out.append(newLine);
		out.append("\tr.e = -6 - a.e;");
		out.append(newLine);
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) || (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 1000;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 3;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) || (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 1000) || (r.b &lt; 0 &amp;&amp; r.b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 10;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("time_t power(int a, int b) { // a ^ b (b &gt;= 0)"); // Integer power
		out.append(newLine);
		out.append("\ttime_t r = 1;");
		out.append(newLine);
		out.append("\twhile (b &gt; 0) {");
		out.append(newLine);
		out.append("\t\tr = r * a;");
		out.append(newLine);
		out.append("\t\tb = b - 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("time_t round(double_t a) { // double --&gt; integer"); // Round
		out.append(newLine);
		out.append("\tif (a.e &lt; -3) {");
		out.append(newLine);
		out.append("\t\tif (a.b &lt; 5000) return 0;");
		out.append(newLine);
		out.append("\t\telse return 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e == -1) {");
		out.append(newLine);
		out.append("\t\tif (a.b % 10 &lt; 5) {");
		out.append(newLine);
		out.append("\t\t\treturn a.b / 10;");
		out.append(newLine);
		out.append("\t\t} else {");
		out.append(newLine);
		out.append("\t\t\treturn 1 + a.b / 10;");
		out.append(newLine);
		out.append("\t\t}");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e == -2) {");
		out.append(newLine);
		out.append("\t\tif (a.b % 100 &lt; 50) {");
		out.append(newLine);
		out.append("\t\t\treturn a.b / 100;");
		out.append(newLine);
		out.append("\t\t} else {");
		out.append(newLine);
		out.append("\t\t\treturn 1 + a.b / 100;");
		out.append(newLine);
		out.append("\t\t}");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e == -3) {");
		out.append(newLine);
		out.append("\t\tif (a.b % 1000 &lt; 500) {");
		out.append(newLine);
		out.append("\t\t\treturn a.b / 1000;");
		out.append(newLine);
		out.append("\t\t} else {");
		out.append(newLine);
		out.append("\t\t\treturn 1 + a.b / 1000;");
		out.append(newLine);
		out.append("\t\t}");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn a.b * power(10, a.e);");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append(newLine);
		out.append("double_t scenario1(double_t k, double_t r1, double_t r1Levels, bool r1Active) {"); // Scenario 1
		out.append(newLine);
		out.append("\tdouble_t E;");
		out.append(newLine);
		out.append("\tif (r1Active) { //If we depend on active R1, the level of activity is the value of E");
		out.append(newLine);
		out.append("\t\tE = r1;");
		out.append(newLine);
		out.append("\t} else { //otherwise we find the inactivity level via the total number of levels");
		out.append(newLine);
		out.append("\t\tE = subtract(r1Levels, r1);");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn multiply(k, E);");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double_t scenario2_3(double_t k, double_t r2, double_t r2Levels, bool r2Active, double_t r1, double_t r1Levels, bool r1Active) {"); // Scenarios 2 and 3
		out.append(newLine);
		out.append("\tdouble_t E, S;");
		out.append(newLine);
		out.append("\tif (r1Active) { //If we depend on active R1, the level of activity is the value of E");
		out.append(newLine);
		out.append("\t\tE = r1;");
		out.append(newLine);
		out.append("\t} else { //otherwise we find the inactivity level via the total number of levels");
		out.append(newLine);
		out.append("\t\tE = subtract(r1Levels, r1);");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (r2Active) { //Same for R2");
		out.append(newLine);
		out.append("\t\tS = r2;");
		out.append(newLine);
		out.append("\t} else {");
		out.append(newLine);
		out.append("\t\tS = subtract(r2Levels, r2);");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn multiply(k, multiply(E, S));");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double_t int_to_double(int a) { //Used to translate an activity level into double."); // Translate an int to double
		out.append(newLine);
		out.append("\tdouble_t r;");
		out.append(newLine);
		out.append("\tif (a &lt; 10) {");
		out.append(newLine);
		out.append("\t\tr.b = a * 1000;");
		out.append(newLine);
		out.append("\t\tr.e = -3;");
		out.append(newLine);
		out.append("\t} else if (a &lt; 100) {");
		out.append(newLine);
		out.append("\t\tr.b = a * 100;");
		out.append(newLine);
		out.append("\t\tr.e = -2;");
		out.append(newLine);
		out.append("\t} else if (a &lt; 1000) {");
		out.append(newLine);
		out.append("\t\tr.b = a * 10;");
		out.append(newLine);
		out.append("\t\tr.e = -1;");
		out.append(newLine);
		out.append("\t} else if (a &lt; 10000) { //Our model supports up to 100 levels, so this should be the most it makes sense to check");
		out.append(newLine);
		out.append("\t\tr.b = a;");
		out.append(newLine);
		out.append("\t\tr.e = 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		
		out.append("</declaration>");
		
		out.append(newLine);
		out.append(newLine);
		// output templates
		if (uncertainty != 0) {
			this.appendTemplates(out, m);
		} else {
			this.appendDeterministicTemplates(out, m);
		}
		
		out.append(newLine);
		out.append("<system>");
		out.append(newLine);
		
		for (Reactant r : m.getSortedReactantList()) {
			if (!r.get(ENABLED).as(Boolean.class) || !r.get(HAS_INFLUENCING_REACTIONS).as(Boolean.class)) continue;
			this.appendReactionProcess(out, m, r, reactantIndex);
		}
		
		out.append(newLine);
		out.append(newLine);
		out.append(newLine);
		
		// compose the system
		out.append("system ");
		Iterator<Reactant> iter = m.getSortedReactantList().iterator();
		boolean first = true;
		while (iter.hasNext()) {
			Reactant r = iter.next();
			if (!r.get(ENABLED).as(Boolean.class) || !r.get(HAS_INFLUENCING_REACTIONS).as(Boolean.class)) continue;
			if (!first) {
				out.append(", ");
			}
			out.append(r.getId() + "_");
			first = false;
		}
		out.append(";");
		
		out.append(newLine);
		out.append(newLine);
		out.append("</system>");
		out.append(newLine);
		out.append("</nta>");
	}
	
	private void appendReactionProcess(StringBuilder out, Model m, Reactant r, int index) {
		out.append(r.getId() + "_ = Reactant_" + r.getId() + "(" + r.getId() + ", " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + ");");
		out.append(newLine);
	}
	
	private void appendReactionTables(StringBuilder out, Model m, Reaction r) {
		String r1Id = r.get(CATALYST).as(String.class);
		String r2Id = r.get(REACTANT).as(String.class);
		String rOutput = r.get(Model.Properties.OUTPUT_REACTANT).as(String.class);
		out.append("//Reaction " + r1Id + " (" + m.getReactant(r1Id).get(ALIAS).as(String.class) + ") " + (!r2Id.equals(rOutput)?("AND " + r2Id + " (" + m.getReactant(r2Id).get(ALIAS).as(String.class) + ") "):"") + (r.get(INCREMENT).as(Integer.class)>0?"-->":"--|") + " " + (r2Id.equals(rOutput)?(r2Id + " (" + m.getReactant(r2Id).get(ALIAS).as(String.class) + ")"):(rOutput + " (" + m.getReactant(rOutput).get(ALIAS).as(String.class) + ")")));
		out.append(newLine);
		
		out.append("timeActivity " + r.getId() + ";");
		out.append(newLine);
		out.append("const double_t k_" + r.getId() + " = " + formatDouble(r.get(Model.Properties.SCENARIO_PARAMETER_K).as(Double.class) / (m.getProperties().get(Model.Properties.SECS_POINT_SCALE_FACTOR).as(Double.class) * r.get(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").as(Double.class))) + ";");
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
			
			for (Reactant r : m.getSortedReactantList()) {
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
				
				StringBuilder template = new StringBuilder("<template><name>Reactant_" + r.getId() + "</name><parameter>int&amp; R, const int MAX</parameter><declaration>int[-1, 1] delta, deltaNew = 0, deltaOld = 0, deltaOldOld = 0, deltaOldOldOld = 0;\nbool deltaAlternating = false;\ntime_t tL, tU;\nclock c;\ndouble_t totalRate;\n\n\nvoid updateDeltaOld() {\n\tdeltaOldOldOld = deltaOldOld;\n\tdeltaOldOld = deltaOld;\n\tdeltaOld = deltaNew;\n\tdeltaNew = delta;\n\tdeltaAlternating = false;\n\tif (deltaOldOldOld != 0) { //We have updated delta at least 4 times, so we can see whether we have an oscillation\n\t\tif (deltaNew == deltaOldOld &amp;&amp; deltaOld == deltaOldOldOld &amp;&amp; deltaNew != deltaOld) { //Pairwise equal and alternating (e.g. +1, -1, +1, -1): we are oscillating\n\t\t\tdeltaAlternating = true;\n\t\t\tdeltaNew = deltaOld = deltaOldOld = deltaOldOldOld = 0;\n\t\t}\n\t}\n}\n\nvoid update() {\n");
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
					switch (scenario) {
						case 0:
							template.append("\tdouble_t " + re.getId() + "_r = scenario1(k_" + re.getId() + ", int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "), int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "Levels), " + activeR1 + ");\n");
							break;
						case 1: case 2:
							template.append("\tdouble_t " + re.getId() + "_r = scenario2_3(k_" + re.getId() + ", int_to_double(" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "), int_to_double(" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "Levels), " + activeR2 + ", int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "), int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "Levels), " + activeR1 + ");\n");
							break;
						default:
							break;
					}
				}
				template.append("\ttotalRate = ");
				if (influencingReactions.size() == 1) {
					Reaction re = influencingReactions.get(0);
					int increment = re.get(INCREMENT).as(Integer.class);
					String subtr1 = (increment > 0)?"":"subtract(zero, ",
						   subtr2 = (increment > 0)?"":")";
					template.append(subtr1 + re.getId() + "_r" + subtr2 + ";\n");
					if (uncertainty != 0) {
						template.append("\t" + re.getId() + ".T = round(multiply(inverse(" + re.getId() + "_r), LOWER_UNC));\n");
					} else {
						template.append("\t" + re.getId() + ".T = round(inverse(" + re.getId() + "_r));\n");
					}
				} else {
					StringBuilder computation = new StringBuilder("zero");
					for (Reaction re : influencingReactions) {
						computation.append(", " + re.getId() + "_r)");
						if (re.get(Model.Properties.INCREMENT).as(Integer.class) > 0) {
							computation.insert(0, "add(");
						} else {
							computation.insert(0, "subtract(");
						}
					}
					template.append(computation);
					template.append(";\n");
					for (Reaction re : influencingReactions) {
						if (uncertainty != 0) {
							template.append("\t" + re.getId() + ".T = " + "round(multiply(inverse(" + re.getId() + "_r), LOWER_UNC));\n");
						} else {
							template.append("\t" + re.getId() + ".T = " + "round(inverse(" + re.getId() + "_r));\n");
						}
					}
				}
				template.append("\tif (totalRate.b &lt; 0) {\n\t\tdelta = -1;\n\t\ttotalRate.b = -totalRate.b;\n\t} else {\n\t\tdelta = 1;\n\t}\n\tif (totalRate.b != 0) {\n");
				if (uncertainty != 0) {
					template.append("\t\ttL = round(multiply(inverse(totalRate), LOWER_UNC));\n\t\ttU = round(multiply(inverse(totalRate), UPPER_UNC));\n");
				} else {
					template.append("\t\ttL = round(inverse(totalRate));\n\t\ttU = tL;\n");
				}
				boolean dAlternating = false;
				if (m.getProperties().has("deltaAlternating") && m.getProperties().get("deltaAlternating").as(Boolean.class)) {
					dAlternating = true;
				}
				boolean useOldResetting = false;
				if (m.getProperties().has("useOldResetting") && m.getProperties().get("useOldResetting").as(Boolean.class)) {
					useOldResetting = true;
				}
				template.append("\t} else {\n\t\ttL = INFINITE_TIME;\n\t\ttU = INFINITE_TIME;\n\t}\n\tif (tL != INFINITE_TIME &amp;&amp; tL &gt; tU) { //We use rounded things: maybe the difference between tL and tU was not so great, and with some rounding problems we could have this case\n\t\ttL = tU;\n\t}\n}\n\nvoid react() {\n\tif (0 &lt;= R + delta &amp;&amp; R + delta &lt;= MAX) {\n\t\tR = R + delta;\n\t}\n\t" + (dAlternating?"":"//") + "updateDeltaOld();\n\tupdate();\n}\n\nbool can_react() {\n\treturn !deltaAlternating &amp;&amp; (tL != INFINITE_TIME &amp;&amp; tL != 0 &amp;&amp; tU != 0 &amp;&amp; ((delta &gt;= 0 &amp;&amp; R &lt; MAX) || (delta &lt; 0 &amp;&amp; R &gt; 0)));\n}\n\nbool cant_react() {\n\treturn deltaAlternating || (tL == INFINITE_TIME || tL == 0 || tU == 0 || (delta &gt;= 0 &amp;&amp; R == MAX) || (delta &lt; 0 &amp;&amp; R == 0));\n}</declaration>");
				template.append("<location id=\"id0\" x=\"-1896\" y=\"-728\"><name x=\"-1960\" y=\"-752\">stubborn</name><committed/></location><location id=\"id6\" x=\"-1256\" y=\"-728\"><name x=\"-1248\" y=\"-752\">start</name><committed/></location><location id=\"id7\" x=\"-1552\" y=\"-856\"><name x=\"-1656\" y=\"-872\">not_reacting</name>" + (useOldResetting?"":"<label kind=\"invariant\" x=\"-1656\" y=\"-856\">c'==0</label>") + "</location><location id=\"id8\" x=\"-1416\" y=\"-728\"><name x=\"-1400\" y=\"-752\">updating</name><committed/></location><location id=\"id9\" x=\"-1664\" y=\"-728\"><name x=\"-1728\" y=\"-744\">waiting</name><label kind=\"invariant\" x=\"-1728\" y=\"-720\">c &lt;= tU\n|| tU ==\nINFINITE_TIME</label></location><init ref=\"id6\"/><transition><source ref=\"id8\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1640\" y=\"-760\">(tU == INFINITE_TIME\n|| c &lt;= tU) &amp;&amp; can_react()</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1640\" y=\"-776\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "</transition><transition><source ref=\"id8\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1608\" y=\"-712\">(tU != INFINITE_TIME\n&amp;&amp; c &gt; tU) &amp;&amp; can_react()</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1608\" y=\"-664\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<label kind=\"assignment\" x=\"-1608\" y=\"-680\">c := tU</label><nail x=\"-1528\" y=\"-680\"/><nail x=\"-1608\" y=\"-680\"/></transition><transition><source ref=\"id0\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1816\" y=\"-632\">c &lt; tL</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1816\" y=\"-600\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<label kind=\"assignment\" x=\"-1816\" y=\"-616\">update()</label><nail x=\"-1848\" y=\"-616\"/><nail x=\"-1464\" y=\"-616\"/></transition><transition><source ref=\"id0\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1816\" y=\"-680\">c &gt;= tL</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1816\" y=\"-664\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<nail x=\"-1840\" y=\"-664\"/><nail x=\"-1744\" y=\"-664\"/></transition><transition><source ref=\"id6\"/><target ref=\"id8\"/>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1344\" y=\"-744\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<label kind=\"assignment\" x=\"-1344\" y=\"-728\">update()</label></transition>");
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
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update()" + (useOldResetting?", c := 0":"") + "</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						case 1:
						case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update()" + (useOldResetting?", c := 0":"") + "</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							if (reactant.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(reactant)) {
								alreadyOutputReactants.add(reactant);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(REACTANT).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update()" + (useOldResetting?", c := 0":"") + "</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						default:
							break;
					}
				}
				template.append("<transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1512\" y=\"-840\">cant_react()</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1512\" y=\"-856\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<nail x=\"-1416\" y=\"-824\"/><nail x=\"-1552\" y=\"-824\"/></transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1576\" y=\"-816\">c &gt;= tL</label><label kind=\"synchronisation\" x=\"-1584\" y=\"-800\">reacting[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label><label kind=\"assignment\" x=\"-1568\" y=\"-784\">react(), c := 0</label><nail x=\"-1632\" y=\"-784\"/><nail x=\"-1464\" y=\"-784\"/></transition>");
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
				
				y1 = -680;
				y2 = -664;
				y3 = -696;
				incrY = 48;
				alreadyOutputReactants = new Vector<Reactant>(); //Keep trace of which reactants already have a transition for them, because otherwise we get input nondeterminism
				for (Reaction re : influencingReactions) { //Transitions from waiting to stubborn
					int scenario = re.get(SCENARIO).as(Integer.class);
					Reactant catalyst = m.getReactant(re.get(CATALYST).as(String.class)),
							 reactant = m.getReactant(re.get(REACTANT).as(String.class)); //This is not null only when scenario != 0
					switch (scenario) {
						case 0:
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id8\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1408\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1408\" y=\"" + y2 + "\">update()</label><nail x=\"-1416\" y=\"" + y2 + "\"/><nail x=\"-1320\" y=\"" + y2 + "\"/><nail x=\"-1320\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						case 1:
						case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id8\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1408\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1408\" y=\"" + y2 + "\">update()</label><nail x=\"-1416\" y=\"" + y2 + "\"/><nail x=\"-1320\" y=\"" + y2 + "\"/><nail x=\"-1320\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							if (reactant.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(reactant)) {
								alreadyOutputReactants.add(reactant);
								template.append("<transition><source ref=\"id8\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1408\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(REACTANT).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1408\" y=\"" + y2 + "\">update()</label><nail x=\"-1416\" y=\"" + y2 + "\"/><nail x=\"-1320\" y=\"" + y2 + "\"/><nail x=\"-1320\" y=\"" + y3 + "\"/></transition>");
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
	
	protected void appendDeterministicTemplates(StringBuilder out, Model m) {
		try {
			StringWriter outString;
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document;
			Transformer tra = TransformerFactory.newInstance().newTransformer();
			tra.setOutputProperty(OutputKeys.INDENT, "yes");
			tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			for (Reactant r : m.getSortedReactantList()) {
				if (!r.get(ENABLED).as(Boolean.class)) continue;
				int myIndex = r.get(REACTANT_INDEX).as(Integer.class);
				outString = new StringWriter();
				int upstreamReactants = 0;
				Vector<Reaction> influencingReactions = new Vector<Reaction>();
				for (Reaction re : m.getReactionCollection()) {
					if (re.get(OUTPUT_REACTANT).as(String.class).equals(r.getId()))  { //If the reactant is downstream of a reaction, count that reaction
						influencingReactions.add(re);
						if (!re.get(CATALYST).as(String.class).equals(r.getId())) { //Also count all upstream reactants different from itself (an autoarc represents an influencing reaction in which the output reactant is also the catalyst, so we need to distinguish)
							upstreamReactants++;
						}
						if (re.get(SCENARIO).as(Integer.class) == 2 && !re.get(REACTANT).as(String.class).equals(r.getId())) {
							upstreamReactants++;
						}
					}
				}
				
				if (influencingReactions.size() < 1) {
					r.let(HAS_INFLUENCING_REACTIONS).be(false);
					continue;
				}
				r.let(HAS_INFLUENCING_REACTIONS).be(true);
				
				StringBuilder template = new StringBuilder("<template><name>Reactant_" + r.getId() + "</name><parameter>int&amp; R, const int MAX</parameter><declaration>int[-1, 1] delta;\ndouble_t totalRate;\n\n\nvoid update() {\n");
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
					switch (scenario) {
						case 0:
							template.append("\tdouble_t " + re.getId() + "_r = scenario1(k_" + re.getId() + ", int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "), int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "Levels), " + activeR1 + ");\n");
							break;
						case 1: case 2:
							template.append("\tdouble_t " + re.getId() + "_r = scenario2_3(k_" + re.getId() + ", int_to_double(" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "), int_to_double(" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "Levels), " + activeR2 + ", int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "), int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "Levels), " + activeR1 + ");\n");
							break;
						default:
							break;
					}
				}
				template.append("\ttotalRate = ");
				if (influencingReactions.size() == 1) {
					Reaction re = influencingReactions.get(0);
					int increment = re.get(INCREMENT).as(Integer.class);
					String subtr1 = (increment > 0)?"":"subtract(zero, ",
						   subtr2 = (increment > 0)?"":")";
					template.append(subtr1 + re.getId() + "_r" + subtr2 + ";\n");
					template.append("\t" + re.getId() + ".T = round(inverse(" + re.getId() + "_r));\n");
				} else {
					StringBuilder computation = new StringBuilder("zero");
					for (Reaction re : influencingReactions) {
						computation.append(", " + re.getId() + "_r)");
						if (re.get(Model.Properties.INCREMENT).as(Integer.class) > 0) {
							computation.insert(0, "add(");
						} else {
							computation.insert(0, "subtract(");
						}
					}
					template.append(computation);
					template.append(";\n");
					for (Reaction re : influencingReactions) {
						template.append("\t" + re.getId() + ".T = " + "round(inverse(" + re.getId() + "_r));\n");
					}
				}
				template.append("\tif (totalRate.b &lt; 0) {\n\t\tdelta = -1;\n\t\ttotalRate.b = -totalRate.b;\n\t} else {\n\t\tdelta = 1;\n\t}\n\tif (totalRate.b != 0) {\n");
				template.append("\t\tT[" + myIndex + "] = round(inverse(totalRate));\n");
				template.append("\t} else {\n\t\tT[" + myIndex + "] = INFINITE_TIME;\n\t}\n}\n\nvoid react() {\n\tif (0 &lt;= R + delta &amp;&amp; R + delta &lt;= MAX) {\n\t\tR = R + delta;\n\t}\n\tupdate();\n}\n\nbool can_react() {\n\treturn T[" + myIndex + "] != INFINITE_TIME &amp;&amp; T[" + myIndex + "] != 0 &amp;&amp; ((delta &gt;= 0 &amp;&amp; R &lt; MAX) || (delta &lt; 0 &amp;&amp; R &gt; 0));\n}\n\nbool cant_react() {\n\treturn T[" + myIndex + "] == INFINITE_TIME || T[" + myIndex + "] == 0 || (delta &gt;= 0 &amp;&amp; R == MAX) || (delta &lt; 0 &amp;&amp; R == 0);\n}</declaration>");
				template.append("<location id=\"id0\" x=\"-1896\" y=\"-728\"><name x=\"-1960\" y=\"-752\">stubborn</name><committed/></location>");
				template.append("<location id=\"id1\" x=\"-1720\" y=\"-856\"><committed/></location>");
				template.append("<location id=\"id6\" x=\"-1256\" y=\"-728\"><name x=\"-1248\" y=\"-752\">start</name><committed/></location>");
				template.append("<location id=\"id7\" x=\"-1456\" y=\"-608\"><name x=\"-1448\" y=\"-632\">not_reacting</name><label kind=\"invariant\" x=\"-1504\" y=\"-600\">c[" + myIndex + "]'==0</label></location>");
				template.append("<location id=\"id8\" x=\"-1392\" y=\"-728\"><name x=\"-1400\" y=\"-752\">updating</name><committed/></location>");
				template.append("<location id=\"id9\" x=\"-1664\" y=\"-728\"><name x=\"-1728\" y=\"-744\">waiting</name><label kind=\"invariant\" x=\"-1728\" y=\"-720\">c[" + myIndex + "] &lt;= T[" + myIndex + "]</label></location>");
				if (upstreamReactants > 1) { //If I depend on more than 1 reactant, when one reacts I may have to wait also for the others (in case anybody else reacts in the same instant)
					template.append("<location id=\"id2\" x=\"-1720\" y=\"-928\"><name x=\"-1760\" y=\"-960\">wait_for_others</name></location>");
				}
				template.append("<init ref=\"id6\"/>");
				template.append("<transition><source ref=\"id8\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1640\" y=\"-744\">c[" + myIndex + "] &lt;= T[" + myIndex + "] &amp;&amp; can_react()</label><label kind=\"assignment\" x=\"-1608\" y=\"-728\">active[" + myIndex + "] = true</label></transition>");
				template.append("<transition><source ref=\"id8\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1608\" y=\"-696\">c[" + myIndex + "] &gt; T[" + myIndex + "] &amp;&amp; can_react()</label><label kind=\"assignment\" x=\"-1608\" y=\"-680\">c[" + myIndex + "] := T[" + myIndex + "],\nactive[" + myIndex + "] = true</label><nail x=\"-1528\" y=\"-680\"/><nail x=\"-1608\" y=\"-680\"/></transition>");
				template.append("<transition><source ref=\"id0\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1816\" y=\"-680\">c[" + myIndex + "] &gt;= T[" + myIndex + "]</label><label kind=\"assignment\" x=\"-1824\" y=\"-664\">active[" + myIndex + "] = true</label><nail x=\"-1840\" y=\"-664\"/><nail x=\"-1744\" y=\"-664\"/></transition>");
				template.append("<transition><source ref=\"id6\"/><target ref=\"id8\"/><label kind=\"assignment\" x=\"-1344\" y=\"-728\">update()</label></transition>");
				template.append("<transition><source ref=\"id0\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1976\" y=\"-728\">c[" + myIndex + "] &lt; T[" + myIndex + "]</label><nail x=\"-1968\" y=\"-728\"/><nail x=\"-1968\" y=\"-856\"/></transition>");
				Vector<Reactant> alreadyOutputReactants = new Vector<Reactant>();
				if (upstreamReactants > 1) {
					int y1 = -944,
						y2 = -928,
						incrY = -40;
					alreadyOutputReactants.clear();
					for (Reaction re : influencingReactions) {
						int scenario = re.get(SCENARIO).as(Integer.class);
						Reactant catalyst = m.getReactant(re.get(CATALYST).as(String.class)),
								 reactant = m.getReactant(re.get(REACTANT).as(String.class)); //This is not null only when scenario != 0
						int cataIdx = catalyst.get(REACTANT_INDEX).as(Integer.class),
							reacIdx = reactant.get(REACTANT_INDEX).as(Integer.class);
						switch (scenario) {
							case 0:
								if (cataIdx != myIndex && !alreadyOutputReactants.contains(catalyst)) {
									alreadyOutputReactants.add(catalyst);
									template.append("<transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1696\" y=\"" + y1 + "\">active[" + cataIdx + "] &amp;&amp; c[" + cataIdx + "] &gt;= T[" + cataIdx + "]</label><nail x=\"-1560\" y=\"-904\"/><nail x=\"-1496\" y=\"-904\"/><nail x=\"-1496\" y=\"" + y2 + "\"/><nail x=\"-1696\" y=\"" + y2 + "\"/></transition>");
									template.append("<transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1840\" y=\"" + y1 + "\">reacting[" + cataIdx + "]?</label><nail x=\"-1744\" y=\"" + y2 + "\"/><nail x=\"-1848\" y=\"" + y2 + "\"/><nail x=\"-1848\" y=\"-904\"/></transition>");
									y1 += incrY;
									y2 += incrY;
								}
								break;
							case 1:
							case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
								if (cataIdx != myIndex && !alreadyOutputReactants.contains(catalyst)) {
									template.append("<transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1696\" y=\"" + y1 + "\">active[" + cataIdx + "] &amp;&amp; c[" + cataIdx + "] &gt;= T[" + cataIdx + "]</label><nail x=\"-1560\" y=\"-904\"/><nail x=\"-1496\" y=\"-904\"/><nail x=\"-1496\" y=\"" + y2 + "\"/><nail x=\"-1696\" y=\"" + y2 + "\"/></transition>");
									template.append("<transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1840\" y=\"" + y1 + "\">reacting[" + cataIdx + "]?</label><nail x=\"-1744\" y=\"" + y2 + "\"/><nail x=\"-1848\" y=\"" + y2 + "\"/><nail x=\"-1848\" y=\"-904\"/></transition>");
									y1 += incrY;
									y2 += incrY;
								}
								if (reacIdx != myIndex && !alreadyOutputReactants.contains(reactant)) {
									alreadyOutputReactants.add(reactant);
									template.append("<transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1696\" y=\"" + y1 + "\">active[" + reacIdx + "] &amp;&amp; c[" + reacIdx + "] &gt;= T[" + reacIdx + "]</label><nail x=\"-1560\" y=\"-904\"/><nail x=\"-1496\" y=\"-904\"/><nail x=\"-1496\" y=\"" + y2 + "\"/><nail x=\"-1696\" y=\"" + y2 + "\"/></transition>");
									template.append("<transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1840\" y=\"" + y1 + "\">reacting[" + reacIdx + "]?</label><nail x=\"-1744\" y=\"" + y2 + "\"/><nail x=\"-1848\" y=\"" + y2 + "\"/><nail x=\"-1848\" y=\"-904\"/></transition>");
									y1 += incrY;
									y2 += incrY;
								}
								break;
							default:
								break;
						}
					}
				}
				template.append("<transition><source ref=\"id1\"/><target ref=\"id8\"/>");
				//Now write the guard for the transition id1 (anonymous committed to decide whether this was the last upstream reactant on which we needed to wait) --update()--> id8 (updating)
				if (upstreamReactants > 1) { //The guard is necessary only if we depend on more than one reactant
					alreadyOutputReactants.clear(); //Keep track of the reactants we have already added to the guard, to avoid useless repetitions
					template.append("<label kind=\"guard\" x=\"-1584\" y=\"-872\">");
					boolean firstOutput = true;
					for (Reaction upstreamReaction : influencingReactions) {
						Reactant cata = m.getReactant(upstreamReaction.get(CATALYST).as(String.class)),
								 reac = m.getReactant(upstreamReaction.get(REACTANT).as(String.class));
						int cataIdx = cata.get(REACTANT_INDEX).as(Integer.class),
							reacIdx = reac.get(REACTANT_INDEX).as(Integer.class);
						switch (upstreamReaction.get(SCENARIO).as(Integer.class)) {
							case 0:
								if (cataIdx != myIndex && !alreadyOutputReactants.contains(cata)) {
									alreadyOutputReactants.add(cata);
									if (firstOutput) {
										firstOutput = false;
									} else {
										template.append(" &amp;&amp; ");
									}
									template.append("(!active[" + cataIdx + "] || c[" + cataIdx + "] &lt; T[" + cataIdx + "])");
								}
								break;
							case 1:
							case 2:
								if (cataIdx != myIndex && !alreadyOutputReactants.contains(cata)) {
									alreadyOutputReactants.add(cata);
									if (firstOutput) {
										firstOutput = false;
									} else {
										template.append(" &amp;&amp; ");
									}
									template.append("(!active[" + cataIdx + "] || c[" + cataIdx + "] &lt; T[" + cataIdx + "])");
								}
								if (reacIdx != myIndex && !alreadyOutputReactants.contains(reac)) {
									alreadyOutputReactants.add(reac);
									if (firstOutput) {
										firstOutput = false;
									} else {
										template.append(" &amp;&amp; ");
									}
									template.append("(!active[" + reacIdx + "] || c[" + reacIdx + "] &lt; T[" + reacIdx + "])");
								}
								break;
							default:
								break;
						}
					}
					template.append("</label>");
				}
				template.append("<label kind=\"assignment\" x=\"-1584\" y=\"-856\">update()</label><nail x=\"-1392\" y=\"-856\"/></transition>");
				int y1 = -624,
					y2 = -608,
					y3 = -856,
					incrY = 32;
				alreadyOutputReactants.clear(); //Keep track of reactants that already have a transition to avoid input nondeterminism
				for (Reaction re : influencingReactions) { //Transitions from not_reacting to updating
					int scenario = re.get(SCENARIO).as(Integer.class);
					Reactant catalyst = m.getReactant(re.get(CATALYST).as(String.class)),
							 reactant = m.getReactant(re.get(REACTANT).as(String.class)); //This is not null only when scenario != 0
					switch (scenario) {
						case 0:
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != myIndex && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1688\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><nail x=\"-1480\" y=\"" + y2 + "\"/><nail x=\"-1992\" y=\"" + y2 + "\"/><nail x=\"-1992\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						case 1:
						case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != myIndex && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1688\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><nail x=\"-1480\" y=\"" + y2 + "\"/><nail x=\"-1992\" y=\"" + y2 + "\"/><nail x=\"-1992\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							if (reactant.get(REACTANT_INDEX).as(Integer.class) != myIndex && !alreadyOutputReactants.contains(reactant)) {
								alreadyOutputReactants.add(reactant);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1688\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(REACTANT).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><nail x=\"-1480\" y=\"" + y2 + "\"/><nail x=\"-1992\" y=\"" + y2 + "\"/><nail x=\"-1992\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						default:
							break;
					}
				}
				template.append("<transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1392\" y=\"-664\">cant_react()</label><nail x=\"-1392\" y=\"-608\"/></transition>");
				template.append("<transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1588\" y=\"-816\">c[" + myIndex + "] &gt;= T[" + myIndex + "]</label><label kind=\"synchronisation\" x=\"-1584\" y=\"-800\">reacting[" + myIndex + "]!</label><label kind=\"assignment\" x=\"-1592\" y=\"-784\">react(), c[" + myIndex + "] := 0,\nactive[" + myIndex + "] = false</label><nail x=\"-1632\" y=\"-784\"/><nail x=\"-1464\" y=\"-784\"/></transition>");
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
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != myIndex && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id9\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1832\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1848\" y=\"" + (y1 - 16) + "\">active[" + myIndex + "] = false</label><nail x=\"-1752\" y=\"" + y2 + "\"/><nail x=\"-1840\" y=\"" + y2 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						case 1:
						case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != myIndex && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id9\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1832\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1848\" y=\"" + (y1 - 16) + "\">active[" + myIndex + "] = false</label><nail x=\"-1752\" y=\"" + y2 + "\"/><nail x=\"-1840\" y=\"" + y2 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							if (reactant.get(REACTANT_INDEX).as(Integer.class) != myIndex && !alreadyOutputReactants.contains(reactant)) {
								alreadyOutputReactants.add(reactant);
								template.append("<transition><source ref=\"id9\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1832\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(REACTANT).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1848\" y=\"" + (y1 - 16) + "\">active[" + myIndex + "] = false</label><nail x=\"-1752\" y=\"" + y2 + "\"/><nail x=\"-1840\" y=\"" + y2 + "\"/></transition>");
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
		out.append("const int " + r.getId() + "Levels := " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + ";");
		out.append(newLine);
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

}

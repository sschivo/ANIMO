package inat.analyser.uppaal;

import inat.InatBackend;
import inat.model.Model;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.util.XmlConfiguration;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * Produces an UPPAAL model to be used with the UPPAAL SMC engine.
 * For comments on what the different functions do, refer to the VariablesModel class.
 */
public class VariablesModelReactionCentered extends VariablesModel {

	private static final String REACTANT_INDEX = Model.Properties.REACTANT_INDEX,
								OUTPUT_REACTANT = Model.Properties.OUTPUT_REACTANT,
								SCENARIO = Model.Properties.SCENARIO;
	
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
		out.append(newLine);
		
		int reactantIndex = 0;
		for (Reactant r : m.getReactantCollection()) {
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			r.let(REACTANT_INDEX).be(reactantIndex);
			reactantIndex++; 
			this.appendReactantVariables(out, r);
		}
		
		out.append("typedef struct {");
		out.append(newLine);
		out.append("\tint[-99980001, 99980001] b;");
		out.append(newLine);
		out.append("\tint e;");
		out.append(newLine);
		out.append("} double;");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("typedef int[-1, 1073741822] time_t;");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("const double zero = {0, 0};");
		out.append(newLine);
		out.append("const double INFINITE_TIME_DOUBLE = {-1000, -3}; //INFINITE_TIME translated into double");
		out.append(newLine);
		out.append("const double timeScale = " + formatDouble(m.getProperties().get(Model.Properties.SECS_POINT_SCALE_FACTOR).as(Double.class)) + "; //The timeScale factor");
		out.append(newLine);
		final XmlConfiguration configuration = InatBackend.get().configuration();
		Integer unc = 5;
		try {
			unc = new Integer(configuration.get(XmlConfiguration.UNCERTAINTY_KEY));
		} catch (NumberFormatException ex) {
			unc = 5;
		}
		out.append("const double uncertaintyUpper = " + formatDouble(1.0 + unc / 100.0) + ", //uncertainty");
		out.append(newLine);
		out.append("\t\t\t uncertaintyLower = " + formatDouble(1.0 - unc / 100.0) + ";");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double subtract(double a, double b) { // a - b");
		out.append(newLine);
		out.append("\tdouble r = {-1000, -1000};");
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
		out.append("double add(double a, double b) { // a + b");
		out.append(newLine);
		out.append("\tdouble r = {-1000,-1000};");
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
		out.append("double multiply(double a, double b) { // a * b");
		out.append(newLine);
		out.append("\tdouble r;");
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
		out.append("double inverse(double a) { // 1 / a");
		out.append(newLine);
		out.append("\tdouble r;");
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
		out.append("time_t power(int a, int b) { // a ^ b (b &gt;= 0)");
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
		out.append("time_t round(double a) { // double --&gt; integer");
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
		out.append("");
		out.append(newLine);
		out.append("double scenario1(double k, double r1, double r1Levels, bool r1Active) {");
		out.append(newLine);
		out.append("\tdouble E;");
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
		out.append("double scenario2_3(double k, double r2, double r2Levels, bool r2Active, double r1, double r1Levels, bool r1Active) {");
		out.append(newLine);
		out.append("\tdouble E, S;");
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
		out.append("double int_to_double(int a) { //Used to translate an activity level into double.");
		out.append(newLine);
		out.append("\tdouble r;");
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
		out.append("\t} else if (a &lt; 10000) { //Our model supports up to 100 levels, so this should be the most we can check");
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
		this.appendTemplates(out, m);
		
		out.append(newLine);
		out.append("<system>");
		out.append(newLine);
		
		int reactionIndex = 0;
		for (Reaction r : m.getReactionCollection()) {
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			this.appendReactionProcesses(out, m, r, reactionIndex);
			reactionIndex++;
		}
		out.append(newLine);
		out.append(newLine);

		out.append(newLine);
		out.append(newLine);
		
		// compose the system
		out.append("system ");
		Iterator<Reaction> iter = m.getReactionCollection().iterator();
		boolean first = true;
		while (iter.hasNext()) {
			Reaction r = iter.next();
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			if (!first) {
				out.append(", ");
			}
			out.append(getReactionName(r));
			first = false;
		}
		//out.append(", Crono;");
		out.append(";");
		
		out.append(newLine);
		out.append(newLine);
		out.append("</system>");
		out.append(newLine);
		out.append("</nta>");
	}

	@Override
	protected void appendReactionProcesses(StringBuilder out, Model m, Reaction r, int index) {
		//NOTICE THAT index IS NOT USED HERE!!
		//We used it in the VariablesModel class, and just to maintain the same form, we still take it here, even if it is never used.
		index = -1;
		
		String r1Id = r.get(CATALYST).as(String.class);
		String r2Id = r.get(REACTANT).as(String.class);
		String rOutput = r.get(Model.Properties.OUTPUT_REACTANT).as(String.class);
		out.append("//Reaction " + r1Id + " (" + m.getReactant(r1Id).get(ALIAS).as(String.class) + ") " + (!r2Id.equals(rOutput)?("AND " + r2Id + " (" + m.getReactant(r2Id).get(ALIAS).as(String.class) + ") "):"") + (r.get(INCREMENT).as(Integer.class)>0?"-->":"--|") + " " + (r2Id.equals(rOutput)?(r2Id + " (" + m.getReactant(r2Id).get(ALIAS).as(String.class) + ")"):(rOutput + " (" + m.getReactant(rOutput).get(ALIAS).as(String.class) + ")")));
		out.append(newLine);
		
//		Table timesL, timesU;
//		Property property = r.get(TIMES_LOWER);
//		if (property != null) {
//			timesL = property.as(Table.class);
//		} else {
//			timesL = r.get(TIMES).as(Table.class);
//		}
//		property = r.get(TIMES_UPPER);
//		if (property != null) {
//			timesU = property.as(Table.class);
//		} else {
//			timesU = r.get(TIMES).as(Table.class);
//		}
//
//		assert timesL.getRowCount() == m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + 1 : "Incorrect number of rows in 'times lower' table of '"
//				+ r + "'.";
//		assert timesU.getRowCount() == m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + 1 : "Incorrect number of rows in 'times upper' table of '"
//			+ r + "'.";
//		assert timesL.getColumnCount() == m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + 1 : "Incorrect number of columns in 'times lower' table of '"
//				+ r + "'.";
//		assert timesU.getColumnCount() == m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + 1 : "Incorrect number of columns in 'times upper' table of '"
//			+ r + "'.";
		
//		// output times table constant for this reaction
//		out.append("const int " + r.getId());
//		if (r.get(SCENARIO).as(Integer.class) == 0) {
//			out.append("_tLower[" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
//		} else {
//			out.append("_tLower[" + m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
//		}
//		out.append(newLine);
//		
//		if (r.get(SCENARIO).as(Integer.class) == 0) {
//			if (r.get(INCREMENT).as(Integer.class) >= 0) {
//				// for each column
//				for (int col = 0; col < timesL.getColumnCount(); col++) {
//					out.append(formatTime(timesL.get(0, col)));
//					
//					// seperate value with a comma if it is not the last one
//					if (col < timesL.getColumnCount() - 1) {
//						out.append(", ");
//					}
//				}
//			} else {
//				// for each column
//				for (int col = 0; col < timesL.getColumnCount(); col++) {
//					out.append(formatTime(timesL.get(1, col)));
//					
//					// seperate value with a comma if it is not the last one
//					if (col < timesL.getColumnCount() - 1) {
//						out.append(", ");
//					}
//				}
//			}
//		} else {
//			// for each row
//			for (int row = 0; row < timesL.getRowCount(); row++) {
//				out.append("\t\t{");
//				
//				// for each column
//				for (int col = 0; col < timesL.getColumnCount(); col++) {
//					out.append(formatTime(timesL.get(row, col)));
//					
//					// seperate value with a comma if it is not the last one
//					if (col < timesL.getColumnCount() - 1) {
//						out.append(", ");
//					}
//				}
//				
//				out.append("}");
//	
//				// end row line with a comma if it is not the last one
//				if (row < timesL.getRowCount() - 1) {
//					out.append(",");
//				}
//				out.append(newLine);
//			}
//		}
//
//		out.append("};");
//		out.append(newLine);
//		
//		// output times table constant for this reaction
//		out.append("const int " + r.getId());
//		if (r.get(SCENARIO).as(Integer.class) == 0) {
//			out.append("_tUpper[" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
//		} else {
//			out.append("_tUpper[" + m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
//		}
//		out.append(newLine);
//
//		if (r.get(SCENARIO).as(Integer.class) == 0) {
//			if (r.get(INCREMENT).as(Integer.class) >= 0) {
//				// for each column
//				for (int col = 0; col < timesU.getColumnCount(); col++) {
//					out.append(formatTime(timesU.get(0, col)));
//					
//					// seperate value with a comma if it is not the last one
//					if (col < timesU.getColumnCount() - 1) {
//						out.append(", ");
//					}
//				}
//			} else {
//				// for each column
//				for (int col = 0; col < timesU.getColumnCount(); col++) {
//					out.append(formatTime(timesU.get(1, col)));
//					
//					// seperate value with a comma if it is not the last one
//					if (col < timesU.getColumnCount() - 1) {
//						out.append(", ");
//					}
//				}
//			}
//		} else {
//			// for each row
//			for (int row = 0; row < timesU.getRowCount(); row++) {
//				out.append("\t\t{");
//				
//				// for each column
//				for (int col = 0; col < timesU.getColumnCount(); col++) {
//					out.append(formatTime(timesU.get(row, col)));
//	
//					// seperate value with a comma if it is not the last one
//					if (col < timesU.getColumnCount() - 1) {
//						out.append(", ");
//					}
//				}
//	
//				out.append("}");
//	
//				// end row line with a comma if it is not the last one
//				if (row < timesU.getRowCount() - 1) {
//					out.append(",");
//				}
//				out.append(newLine);
//			}
//		}
//		
//		out.append("};");
//		out.append(newLine);
//		out.append(newLine);
		
		// output process instantiation
		final String name = getReactionName(r);
		out.append(name + " = Reaction_" + name + "(" + r1Id + ", " + r2Id + ", " + rOutput + ", " + /*name + "_tLower, " + name + "_tUpper, " +*/ r.get(INCREMENT).as(Integer.class)
				+ ", reacting[" + m.getReactant(r1Id).get(REACTANT_INDEX).as(Integer.class) + "], reacting[" + m.getReactant(r2Id).get(REACTANT_INDEX).as(Integer.class) + "]"
				+ ", reacting[" + m.getReactant(rOutput).get(REACTANT_INDEX).as(Integer.class) + "]);");
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
			for (Reaction r : m.getReactionCollection()) {
				if (!r.get(ENABLED).as(Boolean.class)) continue;
				outString = new StringWriter();
				if (r.get(CATALYST).as(String.class).equals(r.get(REACTANT).as(String.class)) && r.get(SCENARIO).as(Integer.class) != 0) { //R1 == R2: use only r1_reacting to avoid input nondeterminism
					boolean activeR2 = true;
					if (r.get(INCREMENT).as(Integer.class) >= 0) {
						activeR2 = false;
					}
					document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + ((r.get(OUTPUT_REACTANT).as(String.class).equals(r.get(REACTANT).as(String.class))) ? "" : "_" + r.get(OUTPUT_REACTANT).as(String.class)) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output, const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>clock c;\ntime_t T, tL, tU;\nconst double k = " + formatDouble(r.getProperties().get(Model.Properties.SCENARIO_PARAMETER_K).as(Double.class)) + ";\nconst double levelsScale = " + formatDouble(r.get(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").as(Double.class)) + ";\nconst double scale = multiply(levelsScale, timeScale);\nconst double R1Levels = " + formatDouble(1.0 * m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class)) + ",\n\t\t\t R2Levels = " + formatDouble(1.0 * m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class)) + ";\ndouble r;\n\nvoid update() {\n\tr = scenario2_3(k, int_to_double(reactant2), R2Levels, " + (activeR2?"true":"false") + ", int_to_double(reactant1), R1Levels, true);\n\tif (r.b != 0) {\n\t\tdouble time = multiply(inverse(r), scale);\n\t\ttL = round(multiply(time, uncertaintyLower));\n\t\ttU = round(multiply(time, uncertaintyUpper));\n\t} else {\n\t\ttL = INFINITE_TIME;\n\t\ttU = INFINITE_TIME;\n\t}\n\tif (tL != INFINITE_TIME &amp;&amp; tU != INFINITE_TIME &amp;&amp; tL &gt; tU) { //We use rounded things: maybe the difference between tL and tU was not so great, and with some rounding problems we could have this case\n\t\ttL = tU;\n\t}\n}\n\nvoid react() {\n\toutput = output + delta;\n\tupdate();\n}\n</declaration><location id=\"id0\" x=\"-1512\" y=\"-696\"><name x=\"-1584\" y=\"-688\">stubborn</name><committed/></location><location id=\"id1\" x=\"-1384\" y=\"-976\"><name x=\"-1392\" y=\"-1008\">not_reacting</name></location><location id=\"id2\" x=\"-960\" y=\"-856\"><name x=\"-952\" y=\"-880\">resetting</name><committed/></location><location id=\"id3\" x=\"-1384\" y=\"-768\"><name x=\"-1416\" y=\"-808\">reacting</name><label kind=\"invariant\" x=\"-1544\" y=\"-792\">tU == INFINITE_TIME\n|| c&lt;=tU</label></location><location id=\"id4\" x=\"-1384\" y=\"-896\"><committed/></location><location id=\"id5\" x=\"-1248\" y=\"-624\"><name x=\"-1360\" y=\"-616\">about_to_react</name></location><location id=\"id6\" x=\"-1536\" y=\"-896\"><name x=\"-1568\" y=\"-928\">start</name><committed/></location><init ref=\"id6\"/><transition><source ref=\"id0\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1568\" y=\"-656\">c&lt;tL</label><label kind=\"assignment\" x=\"-1568\" y=\"-640\">update()</label><nail x=\"-1512\" y=\"-592\"/><nail x=\"-928\" y=\"-592\"/><nail x=\"-928\" y=\"-776\"/></transition><transition><source ref=\"id0\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1568\" y=\"-728\">c&gt;=tL</label><nail x=\"-1512\" y=\"-744\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1496\" y=\"-736\">r1_reacting?</label><nail x=\"-1408\" y=\"-720\"/><nail x=\"-1488\" y=\"-720\"/></transition><transition><source ref=\"id5\"/><target ref=\"id3\"/><label kind=\"synchronisation\" x=\"-1272\" y=\"-672\">output_reacting?</label><label kind=\"assignment\" x=\"-1272\" y=\"-656\">c := tU</label><nail x=\"-1144\" y=\"-624\"/><nail x=\"-1144\" y=\"-680\"/><nail x=\"-1344\" y=\"-680\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-1016\">update(), c:=0</label><nail x=\"-1416\" y=\"-1000\"/><nail x=\"-1672\" y=\"-1000\"/><nail x=\"-1672\" y=\"-576\"/><nail x=\"-912\" y=\"-576\"/><nail x=\"-912\" y=\"-792\"/></transition><transition><source ref=\"id3\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1448\" y=\"-672\">c&gt;=tL\n&amp;&amp; (output+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "\n|| output+delta&lt;0)</label><nail x=\"-1384\" y=\"-680\"/><nail x=\"-1456\" y=\"-680\"/><nail x=\"-1456\" y=\"-624\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1128\" y=\"-736\">c&gt;=tL\n&amp;&amp; output+delta&gt;=0\n&amp;&amp; output+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1128\" y=\"-696\">output_reacting!</label><label kind=\"assignment\" x=\"-1128\" y=\"-680\">react(),\nc:=0</label><nail x=\"-1320\" y=\"-696\"/><nail x=\"-936\" y=\"-696\"/><nail x=\"-936\" y=\"-768\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1328\" y=\"-992\">tL == INFINITE_TIME</label><nail x=\"-960\" y=\"-976\"/></transition><transition><source ref=\"id4\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1448\" y=\"-952\">tL == INFINITE_TIME</label></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-896\">tU != INFINITE_TIME\n&amp;&amp; c&gt;tU</label><label kind=\"assignment\" x=\"-1320\" y=\"-872\">c:=tU, T:=tL</label><nail x=\"-1328\" y=\"-856\"/></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-824\">(tU == INFINITE_TIME\n&amp;&amp; tL != INFINITE_TIME)\n|| (tU != INFINITE_TIME\n&amp;&amp; c&lt;=tU)</label><label kind=\"assignment\" x=\"-1320\" y=\"-768\">T:=tL</label><nail x=\"-960\" y=\"-752\"/><nail x=\"-1328\" y=\"-752\"/></transition><transition><source ref=\"id4\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1496\" y=\"-864\">tL != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1496\" y=\"-848\">T := tL, c := 0</label></transition><transition><source ref=\"id6\"/><target ref=\"id4\"/><label kind=\"assignment\" x=\"-1496\" y=\"-912\">update()</label></transition></template>").getBytes()));
				} else if (r.get(SCENARIO).as(Integer.class) == 0) { //If the scenario is 0, we depend only on reactant1, so we don't need to refer to reactant2 (both as in using a monodimensional table and not receiving input on the r2_reacting channel)
					document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + ((r.get(OUTPUT_REACTANT).as(String.class).equals(r.get(REACTANT).as(String.class))) ? "" : "_" + r.get(OUTPUT_REACTANT).as(String.class)) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output, const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>clock c;\ntime_t T, tL, tU;\nconst double k = " + formatDouble(r.getProperties().get(Model.Properties.SCENARIO_PARAMETER_K).as(Double.class)) + ";\nconst double levelsScale = " + formatDouble(r.get(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").as(Double.class)) + ";\nconst double scale = multiply(levelsScale, timeScale);\nconst double R1Levels = " + formatDouble(1.0 * m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class)) + ";\ndouble r;\n\nvoid update() {\n\tr = scenario1(k, int_to_double(reactant1), R1Levels, true);\n\tif (r.b != 0) {\n\t\tdouble time = multiply(inverse(r), scale);\n\t\ttL = round(multiply(time, uncertaintyLower));\n\t\ttU = round(multiply(time, uncertaintyUpper));\n\t} else {\n\t\ttL = INFINITE_TIME;\n\t\ttU = INFINITE_TIME;\n\t}\n\tif (tL != INFINITE_TIME &amp;&amp; tU != INFINITE_TIME &amp;&amp; tL &gt; tU) { //We use rounded things: maybe the difference between tL and tU was not so great, and with some rounding problems we could have this case\n\t\ttL = tU;\n\t}\n}\n\nvoid react() {\n\toutput = output + delta;\n\tupdate();\n}\n</declaration><location id=\"id0\" x=\"-1512\" y=\"-696\"><name x=\"-1584\" y=\"-688\">stubborn</name><committed/></location><location id=\"id1\" x=\"-1384\" y=\"-976\"><name x=\"-1392\" y=\"-1008\">not_reacting</name></location><location id=\"id2\" x=\"-960\" y=\"-856\"><name x=\"-952\" y=\"-880\">resetting</name><committed/></location><location id=\"id3\" x=\"-1384\" y=\"-768\"><name x=\"-1416\" y=\"-808\">reacting</name><label kind=\"invariant\" x=\"-1544\" y=\"-792\">tU == INFINITE_TIME\n|| c&lt;=tU</label></location><location id=\"id4\" x=\"-1384\" y=\"-896\"><committed/></location><location id=\"id5\" x=\"-1248\" y=\"-624\"><name x=\"-1360\" y=\"-616\">about_to_react</name></location><location id=\"id6\" x=\"-1536\" y=\"-896\"><name x=\"-1568\" y=\"-928\">start</name><committed/></location><init ref=\"id6\"/><transition><source ref=\"id0\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1568\" y=\"-656\">c&lt;tL</label><label kind=\"assignment\" x=\"-1568\" y=\"-640\">update()</label><nail x=\"-1512\" y=\"-592\"/><nail x=\"-928\" y=\"-592\"/><nail x=\"-928\" y=\"-776\"/></transition><transition><source ref=\"id0\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1568\" y=\"-728\">c&gt;=tL</label><nail x=\"-1512\" y=\"-744\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1496\" y=\"-736\">r1_reacting?</label><nail x=\"-1408\" y=\"-720\"/><nail x=\"-1488\" y=\"-720\"/></transition><transition><source ref=\"id5\"/><target ref=\"id3\"/><label kind=\"synchronisation\" x=\"-1272\" y=\"-672\">output_reacting?</label><label kind=\"assignment\" x=\"-1272\" y=\"-656\">c := tU</label><nail x=\"-1144\" y=\"-624\"/><nail x=\"-1144\" y=\"-680\"/><nail x=\"-1344\" y=\"-680\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-1016\">update(), c:=0</label><nail x=\"-1416\" y=\"-1000\"/><nail x=\"-1672\" y=\"-1000\"/><nail x=\"-1672\" y=\"-576\"/><nail x=\"-912\" y=\"-576\"/><nail x=\"-912\" y=\"-792\"/></transition><transition><source ref=\"id3\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1448\" y=\"-672\">c&gt;=tL\n&amp;&amp; (output+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "\n|| output+delta&lt;0)</label><nail x=\"-1384\" y=\"-680\"/><nail x=\"-1456\" y=\"-680\"/><nail x=\"-1456\" y=\"-624\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1128\" y=\"-736\">c&gt;=tL\n&amp;&amp; output+delta&gt;=0\n&amp;&amp; output+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1128\" y=\"-696\">output_reacting!</label><label kind=\"assignment\" x=\"-1128\" y=\"-680\">react(),\nc:=0</label><nail x=\"-1320\" y=\"-696\"/><nail x=\"-936\" y=\"-696\"/><nail x=\"-936\" y=\"-768\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1328\" y=\"-992\">tL == INFINITE_TIME</label><nail x=\"-960\" y=\"-976\"/></transition><transition><source ref=\"id4\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1448\" y=\"-952\">tL == INFINITE_TIME</label></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-896\">tU != INFINITE_TIME\n&amp;&amp; c&gt;tU</label><label kind=\"assignment\" x=\"-1320\" y=\"-872\">c:=tU, T:=tL</label><nail x=\"-1328\" y=\"-856\"/></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-824\">(tU == INFINITE_TIME\n&amp;&amp; tL != INFINITE_TIME)\n|| (tU != INFINITE_TIME\n&amp;&amp; c&lt;=tU)</label><label kind=\"assignment\" x=\"-1320\" y=\"-768\">T:=tL</label><nail x=\"-960\" y=\"-752\"/><nail x=\"-1328\" y=\"-752\"/></transition><transition><source ref=\"id4\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1400\" y=\"-864\">tL != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1496\" y=\"-848\">T := tL, c := 0</label></transition><transition><source ref=\"id6\"/><target ref=\"id4\"/><label kind=\"assignment\" x=\"-1496\" y=\"-912\">update()</label></transition></template>").getBytes()));
				} else { //R1 != R2 and no unidimensional scenario (scen. 0): use both r1_reacting and r2_reacting
					boolean activeR1 = true,
							activeR2 = true;
					if (r.get(SCENARIO).as(Integer.class) == 2) {
						activeR1 = r.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1").as(Boolean.class);
						activeR2 = r.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2").as(Boolean.class);
					} else {
						if (r.get(INCREMENT).as(Integer.class) >= 0) {
							activeR2 = false;
						}
					}
					document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + ((r.get(OUTPUT_REACTANT).as(String.class).equals(r.get(REACTANT).as(String.class))) ? "" : "_" + r.get(OUTPUT_REACTANT).as(String.class)) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output, const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>clock c;\ntime_t T, tL, tU;\nconst double k = " + formatDouble(r.getProperties().get(Model.Properties.SCENARIO_PARAMETER_K).as(Double.class)) + ";\nconst double levelsScale = " + formatDouble(r.get(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").as(Double.class)) + ";\nconst double scale = multiply(levelsScale, timeScale);\nconst double R1Levels = " + formatDouble(1.0 * m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class)) + ",\n\t\t\t R2Levels = " + formatDouble(1.0 * m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class)) + ";\ndouble r;\n\nvoid update() {\n\tr = scenario2_3(k, int_to_double(reactant2), R2Levels, " + (activeR2?"true":"false") + ", int_to_double(reactant1), R1Levels, " + (activeR1?"true":"false") + ");\n\tif (r.b != 0) {\n\t\tdouble time = multiply(inverse(r), scale);\n\t\ttL = round(multiply(time, uncertaintyLower));\n\t\ttU = round(multiply(time, uncertaintyUpper));\n\t} else {\n\t\ttL = INFINITE_TIME;\n\t\ttU = INFINITE_TIME;\n\t}\n\tif (tL != INFINITE_TIME &amp;&amp; tU != INFINITE_TIME &amp;&amp; tL &gt; tU) { //We use rounded things: maybe the difference between tL and tU was not so great, and with some rounding problems we could have this case\n\t\ttL = tU;\n\t}\n}\n\nvoid react() {\n\toutput = output + delta;\n\tupdate();\n}\n</declaration><location id=\"id0\" x=\"-1512\" y=\"-696\"><name x=\"-1584\" y=\"-688\">stubborn</name><committed/></location><location id=\"id1\" x=\"-1384\" y=\"-976\"><name x=\"-1392\" y=\"-1008\">not_reacting</name></location><location id=\"id2\" x=\"-960\" y=\"-856\"><name x=\"-952\" y=\"-880\">resetting</name><committed/></location><location id=\"id3\" x=\"-1384\" y=\"-768\"><name x=\"-1416\" y=\"-808\">reacting</name><label kind=\"invariant\" x=\"-1544\" y=\"-792\">tU == INFINITE_TIME\n|| c&lt;=tU</label></location><location id=\"id4\" x=\"-1384\" y=\"-896\"><committed/></location><location id=\"id5\" x=\"-1248\" y=\"-624\"><name x=\"-1360\" y=\"-616\">about_to_react</name></location><location id=\"id6\" x=\"-1536\" y=\"-896\"><name x=\"-1568\" y=\"-928\">start</name><committed/></location><init ref=\"id6\"/><transition><source ref=\"id0\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1568\" y=\"-656\">c&lt;tL</label><label kind=\"assignment\" x=\"-1568\" y=\"-640\">update()</label><nail x=\"-1512\" y=\"-592\"/><nail x=\"-928\" y=\"-592\"/><nail x=\"-928\" y=\"-776\"/></transition><transition><source ref=\"id0\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1568\" y=\"-728\">c&gt;=tL</label><nail x=\"-1512\" y=\"-744\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-992\">r2_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-992\">update(), c:=0</label><nail x=\"-1664\" y=\"-976\"/><nail x=\"-1664\" y=\"-584\"/><nail x=\"-920\" y=\"-584\"/><nail x=\"-920\" y=\"-784\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1488\" y=\"-712\">r2_reacting?</label><nail x=\"-1400\" y=\"-696\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1496\" y=\"-736\">r1_reacting?</label><nail x=\"-1408\" y=\"-720\"/><nail x=\"-1488\" y=\"-720\"/></transition><transition><source ref=\"id5\"/><target ref=\"id3\"/><label kind=\"synchronisation\" x=\"-1272\" y=\"-672\">output_reacting?</label><label kind=\"assignment\" x=\"-1272\" y=\"-656\">c := tU</label><nail x=\"-1144\" y=\"-624\"/><nail x=\"-1144\" y=\"-680\"/><nail x=\"-1344\" y=\"-680\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-1016\">update(), c:=0</label><nail x=\"-1416\" y=\"-1000\"/><nail x=\"-1672\" y=\"-1000\"/><nail x=\"-1672\" y=\"-576\"/><nail x=\"-912\" y=\"-576\"/><nail x=\"-912\" y=\"-792\"/></transition><transition><source ref=\"id3\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1448\" y=\"-672\">c&gt;=tL\n&amp;&amp; (output+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "\n|| output+delta&lt;0)</label><nail x=\"-1384\" y=\"-680\"/><nail x=\"-1456\" y=\"-680\"/><nail x=\"-1456\" y=\"-624\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1128\" y=\"-736\">c&gt;=tL\n&amp;&amp; output+delta&gt;=0\n&amp;&amp; output+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1128\" y=\"-696\">output_reacting!</label><label kind=\"assignment\" x=\"-1128\" y=\"-680\">react(),\nc:=0</label><nail x=\"-1320\" y=\"-696\"/><nail x=\"-936\" y=\"-696\"/><nail x=\"-936\" y=\"-768\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1328\" y=\"-992\">tL == INFINITE_TIME</label><nail x=\"-960\" y=\"-976\"/></transition><transition><source ref=\"id4\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1448\" y=\"-952\">tL == INFINITE_TIME</label></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-896\">tU != INFINITE_TIME\n&amp;&amp; c&gt;tU</label><label kind=\"assignment\" x=\"-1320\" y=\"-872\">c:=tU, T:=tL</label><nail x=\"-1328\" y=\"-856\"/></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-824\">(tU == INFINITE_TIME\n&amp;&amp; tL != INFINITE_TIME)\n|| (tU != INFINITE_TIME\n&amp;&amp; c&lt;=tU)</label><label kind=\"assignment\" x=\"-1320\" y=\"-768\">T:=tL</label><nail x=\"-960\" y=\"-752\"/><nail x=\"-1328\" y=\"-752\"/></transition><transition><source ref=\"id4\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1496\" y=\"-864\">tL != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1496\" y=\"-848\">T := tL, c := 0</label></transition><transition><source ref=\"id6\"/><target ref=\"id4\"/><label kind=\"assignment\" x=\"-1496\" y=\"-912\">update()</label></transition></template>").getBytes()));
				}
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
		out.append(newLine);
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

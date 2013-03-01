package inat.analyser.uppaal;

import inat.model.Model;
import inat.model.Property;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.util.Table;

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
		
		
		//The encoding of double numbers with integer variables (exponential notation with 3 significant figures)
		out.append(newLine);
		out.append("typedef struct {");
		out.append(newLine);
		out.append("\tint[-99980001, 99980001] b;"); //99980001 = 9999 * 9999, i.e. the maximum result of a multiplication between two .b of double numbers
		out.append(newLine);
		out.append("\tint e;");
		out.append(newLine);
		out.append("} double;");
		out.append(newLine);
		out.append(newLine);
		out.append("const double zero = {0, 0};");
		out.append(newLine);
		/*out.append("const double INFINITE_TIME_DOUBLE = {-100, -2}; //INFINITE_TIME translated into double");*/
		out.append("const double INFINITE_TIME_DOUBLE = {-1000, -3}; //INFINITE_TIME translated into double");
		out.append(newLine);
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
/*		out.append("double subtract(double a, double b) { // a - b"); // Subtraction
		out.append(newLine);
		out.append("\tdouble r = {-100, -100};");
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
		out.append("\tif ((a.e - b.e) &gt;= 3) return a;");
		out.append(newLine);
		out.append("\tif ((b.e - a.e) &gt;= 3) {");
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
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) &#124;&#124; (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) &#124;&#124; (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
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
		out.append(newLine);
		out.append("double add(double a, double b) { // a + b"); // Addition
		out.append(newLine);
		out.append("\tdouble r = {-100,-100};");
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
		out.append("\tif ((a.e - b.e) &gt;= 3) return a;");
		out.append(newLine);
		out.append("\tif ((b.e - a.e) &gt;= 3) return b;");
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
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) &#124;&#124; (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) &#124;&#124; (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
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
		out.append(newLine);
		out.append("double multiply(double a, double b) { // a * b"); // Multiplication
		out.append(newLine);
		out.append("\tdouble r;");
		out.append(newLine);
		out.append("\tr.b = a.b * b.b;");
		out.append(newLine);
		out.append("\tif (r.b % 100 &lt; 50) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b / 100;");
		out.append(newLine);
		out.append("\t} else {");
		out.append(newLine);
		out.append("\t\tr.b = 1 + r.b / 100;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tr.e = a.e + b.e + 2;");
		out.append(newLine);
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) &#124;&#124; (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) &#124;&#124; (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
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
		out.append(newLine);
		out.append("double inverse(double a) { // 1 / a"); // Inverse
		out.append(newLine);
		out.append("\tdouble r;");
		out.append(newLine);
	    out.append("\tif (a.b == 0) {");
		out.append(newLine);
	    out.append("\t\treturn INFINITE_TIME_DOUBLE;");
		out.append(newLine);
	    out.append("\t}");
		out.append(newLine);
		out.append("\tr.b = 100000 / a.b;");
		out.append(newLine);
		out.append("\tr.e = -5 - a.e;");
		out.append(newLine);
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) &#124;&#124; (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) &#124;&#124; (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
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
		out.append(newLine);
		out.append("time_t pow(int a, int b) { // a ^ b (b &gt;= 0)"); // Integer power
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
		out.append(newLine);
		out.append("time_t round(double a) { // double --&gt; integer"); // Round to integer
		out.append(newLine);
		out.append("\tif (a.e &lt; -2) return 0;");
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
		out.append("\treturn a.b * pow(10, a.e);");
		out.append(newLine);
		out.append("}");
		out.append(newLine);*/
		
		out.append("double subtract(double a, double b) { // a - b"); // Subtraction
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
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double add(double a, double b) { // a + b"); // Addition
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
		out.append("\tr.b = r.b * 10;");
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
		out.append("double multiply(double a, double b) { // a * b"); // Multiplication
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
		out.append("double inverse(double a) { // 1 / a"); // Inverse
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
		out.append("time_t pow(int a, int b) { // a ^ b (b &gt;= 0)"); // Integer power
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
		out.append("time_t round(double a) { // double --&gt; integer"); // Round
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
		out.append("\treturn a.b * pow(10, a.e);");
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
		
		for (Reactant r : m.getReactantCollection()) {
			if (!r.get(ENABLED).as(Boolean.class) || !r.get(HAS_INFLUENCING_REACTIONS).as(Boolean.class)) continue;
			this.appendReactionProcess(out, m, r, reactantIndex);
		}
		
		out.append(newLine);
		out.append(newLine);
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
			out.append(r.getId() + "_");
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
		
		Table timesL, timesU;
		Property property = r.get(TIMES_LOWER);
		if (property != null) {
			timesL = property.as(Table.class);
		} else {
			timesL = r.get(TIMES).as(Table.class);
		}
		property = r.get(TIMES_UPPER);
		if (property != null) {
			timesU = property.as(Table.class);
		} else {
			timesU = r.get(TIMES).as(Table.class);
		}

		assert timesL.getRowCount() == m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + 1 : "Incorrect number of rows in 'times lower' table of '"
				+ r + "'.";
		assert timesU.getRowCount() == m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + 1 : "Incorrect number of rows in 'times upper' table of '"
			+ r + "'.";
		assert timesL.getColumnCount() == m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + 1 : "Incorrect number of columns in 'times lower' table of '"
				+ r + "'.";
		assert timesU.getColumnCount() == m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + 1 : "Incorrect number of columns in 'times upper' table of '"
			+ r + "'.";
		
		// output rates table constant for this reaction
		out.append("const double " + r.getId());
		if (r.get(SCENARIO).as(Integer.class) == 0) {
			out.append("_rLower[" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
		} else {
			out.append("_rLower[" + m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
		}
		out.append(newLine);
		
		if (r.get(SCENARIO).as(Integer.class) == 0) {
			if (r.get(INCREMENT).as(Integer.class) >= 0) {
				// for each column
				for (int col = 0; col < timesL.getColumnCount(); col++) {
					out.append(formatTime(timesL.get(0, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesL.getColumnCount() - 1) {
						out.append(", ");
					}
				}
			} else {
				// for each column
				for (int col = 0; col < timesL.getColumnCount(); col++) {
					out.append(formatTime(timesL.get(1, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesL.getColumnCount() - 1) {
						out.append(", ");
					}
				}
			}
		} else {
			// for each row
			for (int row = 0; row < timesL.getRowCount(); row++) {
				out.append("\t\t{");
				
				// for each column
				for (int col = 0; col < timesL.getColumnCount(); col++) {
					out.append(formatTime(timesL.get(row, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesL.getColumnCount() - 1) {
						out.append(", ");
					}
				}
				
				out.append("}");
	
				// end row line with a comma if it is not the last one
				if (row < timesL.getRowCount() - 1) {
					out.append(",");
				}
				out.append(newLine);
			}
		}

		out.append("};");
		out.append(newLine);
		
		// output times table constant for this reaction
		out.append("const double " + r.getId());
		if (r.get(SCENARIO).as(Integer.class) == 0) {
			out.append("_rUpper[" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
		} else {
			out.append("_rUpper[" + m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
		}
		out.append(newLine);

		if (r.get(SCENARIO).as(Integer.class) == 0) {
			if (r.get(INCREMENT).as(Integer.class) >= 0) {
				// for each column
				for (int col = 0; col < timesU.getColumnCount(); col++) {
					out.append(formatTime(timesU.get(0, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesU.getColumnCount() - 1) {
						out.append(", ");
					}
				}
			} else {
				// for each column
				for (int col = 0; col < timesU.getColumnCount(); col++) {
					out.append(formatTime(timesU.get(1, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesU.getColumnCount() - 1) {
						out.append(", ");
					}
				}
			}
		} else {
			// for each row
			for (int row = 0; row < timesU.getRowCount(); row++) {
				out.append("\t\t{");
				
				// for each column
				for (int col = 0; col < timesU.getColumnCount(); col++) {
					out.append(formatTime(timesU.get(row, col)));
	
					// seperate value with a comma if it is not the last one
					if (col < timesU.getColumnCount() - 1) {
						out.append(", ");
					}
				}
	
				out.append("}");
	
				// end row line with a comma if it is not the last one
				if (row < timesU.getRowCount() - 1) {
					out.append(",");
				}
				out.append(newLine);
			}
		}
		
		out.append("};");
		out.append(newLine);
		
		/*
		// output times table constant for this reaction
		out.append("const int " + r.getId());
		if (r.get(SCENARIO).as(Integer.class) == 0) {
			out.append("_tLower[" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
		} else {
			out.append("_tLower[" + m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
		}
		out.append(newLine);
		
		if (r.get(SCENARIO).as(Integer.class) == 0) {
			if (r.get(INCREMENT).as(Integer.class) >= 0) {
				// for each column
				for (int col = 0; col < timesL.getColumnCount(); col++) {
					out.append(super.formatTime(timesL.get(0, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesL.getColumnCount() - 1) {
						out.append(", ");
					}
				}
			} else {
				// for each column
				for (int col = 0; col < timesL.getColumnCount(); col++) {
					out.append(super.formatTime(timesL.get(1, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesL.getColumnCount() - 1) {
						out.append(", ");
					}
				}
			}
		} else {
			// for each row
			for (int row = 0; row < timesL.getRowCount(); row++) {
				out.append("\t\t{");
				
				// for each column
				for (int col = 0; col < timesL.getColumnCount(); col++) {
					out.append(super.formatTime(timesL.get(row, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesL.getColumnCount() - 1) {
						out.append(", ");
					}
				}
				
				out.append("}");
	
				// end row line with a comma if it is not the last one
				if (row < timesL.getRowCount() - 1) {
					out.append(",");
				}
				out.append(newLine);
			}
		}

		out.append("};");
		out.append(newLine);
		
		// output times table constant for this reaction
		out.append("const int " + r.getId());
		if (r.get(SCENARIO).as(Integer.class) == 0) {
			out.append("_tUpper[" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
		} else {
			out.append("_tUpper[" + m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
		}
		out.append(newLine);

		if (r.get(SCENARIO).as(Integer.class) == 0) {
			if (r.get(INCREMENT).as(Integer.class) >= 0) {
				// for each column
				for (int col = 0; col < timesU.getColumnCount(); col++) {
					out.append(super.formatTime(timesU.get(0, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesU.getColumnCount() - 1) {
						out.append(", ");
					}
				}
			} else {
				// for each column
				for (int col = 0; col < timesU.getColumnCount(); col++) {
					out.append(super.formatTime(timesU.get(1, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesU.getColumnCount() - 1) {
						out.append(", ");
					}
				}
			}
		} else {
			// for each row
			for (int row = 0; row < timesU.getRowCount(); row++) {
				out.append("\t\t{");
				
				// for each column
				for (int col = 0; col < timesU.getColumnCount(); col++) {
					out.append(super.formatTime(timesU.get(row, col)));
	
					// seperate value with a comma if it is not the last one
					if (col < timesU.getColumnCount() - 1) {
						out.append(", ");
					}
				}
	
				out.append("}");
	
				// end row line with a comma if it is not the last one
				if (row < timesU.getRowCount() - 1) {
					out.append(",");
				}
				out.append(newLine);
			}
		}
		
		out.append("};");
		out.append(newLine);
		*/
		
		out.append("timeActivity " + r.getId() + ";");
		out.append(newLine);
		out.append(newLine);
		
//		// output process instantiation
//		final String name = getReactionName(r);
//		out.append(name + " = Reaction_" + name + "(" + r1Id + ", " + r2Id + ", " + rOutput + ", " + name + "_tLower, " + name + "_tUpper, " + r.get(INCREMENT).as(Integer.class)
//				+ ", reacting[" + m.getReactant(r1Id).get(REACTANT_INDEX).as(Integer.class) + "], reacting[" + m.getReactant(r2Id).get(REACTANT_INDEX).as(Integer.class) + "]"
//				+ ", reacting[" + m.getReactant(rOutput).get(REACTANT_INDEX).as(Integer.class) + "]);");
//		out.append(newLine);
//		out.append(newLine);
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
					//m.getMapCytoscapeIDtoReactantID().get(r.get(Model.Properties.CYTOSCAPE_ID).as(String.class))
					if (re.get(OUTPUT_REACTANT).as(String.class).equals(r.getId()))  { //If the reactant is downstream of a reaction, count that reaction
						influencingReactions.add(re);
					}
				}
				
				if (influencingReactions.size() < 1) {
					r.let(HAS_INFLUENCING_REACTIONS).be(false);
					continue;
				}
				r.let(HAS_INFLUENCING_REACTIONS).be(true);
				
				StringBuilder template = new StringBuilder("<template><name>Reactant_" + r.getId() + "</name><parameter>int&amp; R, const int MAX</parameter><declaration>int[-1, 1] delta;\ntime_t tL, tU;\nclock c;\ndouble rateLower, rateUpper;\n\nvoid update() {\n\trateLower = ");
				if (influencingReactions.size() == 1) {
					Reaction re = influencingReactions.get(0);
					int scenario = re.get(SCENARIO).as(Integer.class),
						increment = re.get(INCREMENT).as(Integer.class);
					String subtr1 = (increment > 0)?"":"subtract(zero, ",
						   subtr2 = (increment > 0)?"":")";
					switch (scenario) {
						case 0:
							template.append(subtr1 + re.getId() + "_rLower[" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "]" + subtr2 + ";\n\trateUpper = ");
							template.append(subtr1 + re.getId() + "_rUpper[" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "]" + subtr2 + ";\n\t");
							template.append(re.getId() + ".T = " + "round(inverse(" + re.getId() + "_rLower[" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "]));\n"); //+ re.getId() + "_tLower[" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "];\n");
							break;
						case 1:
						case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
							template.append(subtr1 + re.getId() + "_rLower[" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "][" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "]" + subtr2 + ";\n\trateUpper = ");
							template.append(subtr1 + re.getId() + "_rUpper[" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "][" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "]" + subtr2 + ";\n\t");
							template.append(re.getId() + ".T = " + "round(inverse(" + re.getId() + "_rLower[" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "][" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "]));\n"); //+ re.getId() + "_tLower[" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "][" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "];\n");
							break;
						default:
							break;
					}
				} else {
					StringBuilder computation = new StringBuilder("zero");
					for (Reaction re : influencingReactions) {
						int scenario = re.get(SCENARIO).as(Integer.class);
						switch (scenario) {
							case 0:
								computation.append(", " + re.getId() + "_rLower[" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "])");
								break;
							case 1:
							case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
								computation.append(", " + re.getId() + "_rLower[" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "][" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "])");
								break;
							default:
								break;
						}
						if (re.get(Model.Properties.INCREMENT).as(Integer.class) > 0) {
							computation.insert(0, "add(");
						} else {
							computation.insert(0, "subtract(");
						}
					}
					template.append(computation);
					template.append(";\n\trateUpper = ");
					computation = new StringBuilder("zero");
					for (Reaction re : influencingReactions) {
						int scenario = re.get(SCENARIO).as(Integer.class);
						switch (scenario) {
							case 0:
								computation.append(", " + re.getId() + "_rUpper[" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "])");
								break;
							case 1:
							case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
								computation.append(", " + re.getId() + "_rUpper[" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "][" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "])");
								break;
							default:
								break;
						}
						if (re.get(Model.Properties.INCREMENT).as(Integer.class) > 0) {
							computation.insert(0, "add(");
						} else {
							computation.insert(0, "subtract(");
						}
					}
					computation.append(";\n");
					template.append(computation);
					for (Reaction re : influencingReactions) {
						int scenario = re.get(SCENARIO).as(Integer.class);
						switch (scenario) {
							case 0:
								template.append("\t" + re.getId() + ".T = " + "round(inverse(" + re.getId() + "_rLower[" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "]));\n"); //+ re.getId() + "_tLower[" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "];\n");
								break;
							case 1:
							case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
								template.append("\t" + re.getId() + ".T = " + "round(inverse(" + re.getId() + "_rLower[" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "][" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "]));\n"); //+ re.getId() + "_tLower[" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "][" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "];\n");
								break;
							default:
								break;
						}
					}
				}
				template.append("\tif (rateUpper.b &lt; 0) { //Plese note: the smaller rate is the \"upper\" one, which corresponds to the largest value for time\n\t\tdelta = -1;\n\t\trateLower.b = -rateLower.b;\n\t\trateUpper.b = -rateUpper.b;\n\t} else {\n\t\tdelta = 1;\n\t}\n\tif (rateLower.b != 0) {\n\t\ttL = round(inverse(rateLower));\n\t} else {\n\t\ttL = INFINITE_TIME;\n\t}\n\tif (rateUpper.b != 0) {\n\t\ttU = round(inverse(rateUpper));\n\t} else {\n\t\ttU = INFINITE_TIME;\n\t}\n\tif (tL != INFINITE_TIME &amp;&amp; tU != INFINITE_TIME &amp;&amp; tL &gt; tU) { //We use rounded things: maybe the difference between tL and tU was not so great, and with some rounding problems we could have this case\n\t\ttL = tU;\n\t}\n}\n\nvoid react() {\n\tif (0 &lt;= R + delta &amp;&amp; R + delta &lt;= MAX) {\n\t\tR = R + delta;\n\t}\n\tupdate();\n}\n\nbool can_react() {\n\treturn tL != INFINITE_TIME &amp;&amp; tL != 0 &amp;&amp; tU != 0 &amp;&amp; ((delta &gt; 0 &amp;&amp; R &lt; MAX) || (delta &lt; 0 &amp;&amp; R &gt; 0));\n}\n\nbool cant_react() {\n\treturn tL == INFINITE_TIME || tL == 0 || tU == 0 || (delta &gt; 0 &amp;&amp; R == MAX) || (delta &lt; 0 &amp;&amp; R == 0);\n}</declaration>");
				template.append("<location id=\"id0\" x=\"-1896\" y=\"-728\"><name x=\"-1960\" y=\"-752\">stubborn</name><committed/></location><location id=\"id1\" x=\"-1528\" y=\"-728\"><committed/></location><location id=\"id6\" x=\"-1256\" y=\"-728\"><name x=\"-1248\" y=\"-752\">start</name><committed/></location><location id=\"id7\" x=\"-1552\" y=\"-856\"><name x=\"-1656\" y=\"-872\">not_reacting</name></location><location id=\"id8\" x=\"-1416\" y=\"-728\"><name x=\"-1400\" y=\"-752\">updating</name><committed/></location><location id=\"id9\" x=\"-1664\" y=\"-728\"><name x=\"-1728\" y=\"-744\">waiting</name><label kind=\"invariant\" x=\"-1728\" y=\"-720\">c &lt;= tU\n|| tU ==\nINFINITE_TIME</label></location><init ref=\"id6\"/><transition><source ref=\"id1\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1640\" y=\"-760\">tU == INFINITE_TIME\n|| c &lt;= tU</label></transition><transition><source ref=\"id1\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1608\" y=\"-712\">tU != INFINITE_TIME\n&amp;&amp; c &gt; tU</label><label kind=\"assignment\" x=\"-1608\" y=\"-680\">c := tU</label><nail x=\"-1528\" y=\"-680\"/><nail x=\"-1608\" y=\"-680\"/></transition><transition><source ref=\"id0\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1816\" y=\"-632\">c &lt; tL</label><label kind=\"assignment\" x=\"-1816\" y=\"-616\">update()</label><nail x=\"-1848\" y=\"-616\"/><nail x=\"-1464\" y=\"-616\"/></transition><transition><source ref=\"id0\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1816\" y=\"-680\">c &gt;= tL</label><nail x=\"-1840\" y=\"-664\"/><nail x=\"-1744\" y=\"-664\"/></transition><transition><source ref=\"id6\"/><target ref=\"id8\"/><label kind=\"assignment\" x=\"-1344\" y=\"-728\">update()</label></transition>");
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
				template.append("<transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1512\" y=\"-840\">cant_react()</label><nail x=\"-1416\" y=\"-824\"/><nail x=\"-1552\" y=\"-824\"/></transition><transition><source ref=\"id8\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1512\" y=\"-744\">can_react()</label></transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1576\" y=\"-816\">c &gt;= tL</label><label kind=\"synchronisation\" x=\"-1584\" y=\"-800\">reacting[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label><label kind=\"assignment\" x=\"-1568\" y=\"-784\">react(), c := 0</label><nail x=\"-1632\" y=\"-784\"/><nail x=\"-1464\" y=\"-784\"/></transition>");
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
		/*String r1Id = r.get(CATALYST).as(String.class);
		String r2Id = r.get(REACTANT).as(String.class);
		String rOutput = r.get(Model.Properties.OUTPUT_REACTANT).as(String.class);
		return r1Id + "_" + r2Id + ((rOutput.equals(r2Id))? "" : "_" + rOutput);*/
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
	
	@Override
	protected String formatTime(int time) {
		if (time == INFINITE_TIME) {
			return "{0, 0}";
		} else {
			int b, e;
			double d = 1.0 / time; //time is guaranteed not to be 0, because we use 0 as a signal that rounding is not good enough, and increase time scale
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
	/*protected String formatTime(int time) {
		if (time == INFINITE_TIME) {
			return "{0, 0}";
		} else {
			int b, e;
			double d = 1.0 / time; //time is guaranteed not to be 0, because we use 0 as a signal that rounding is not good enough, and increase time scale
			e = (int)Math.round(Math.log10(d)) - 2;
			b = (int)Math.round(d * Math.pow(10, -e));
			if (b < 10) { //We always want 3 figures
				b = b * 100;
				e = e - 2;
			} else if (b < 100) {
				b = b * 10;
				e = e - 1;
			}
			return "{" + b + ", " + e + "}";
		}
	}*/

}

package inat.analyser.uppaal;

import inat.model.Model;
import inat.model.Property;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.util.Table;

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
public class VariablesModelReactionCenteredTables extends VariablesModel {

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

		//out.append("Crono = crono();");
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
		out.append(newLine);
		
		// output process instantiation
		final String name = getReactionName(r);
		out.append(name + " = Reaction_" + name + "(" + r1Id + ", " + r2Id + ", " + rOutput + ", " + name + "_tLower, " + name + "_tUpper, " + r.get(INCREMENT).as(Integer.class)
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
				//document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output_reactant, const int timeL[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r1, r2;</declaration><location id=\"id0\" x=\"-1328\" y=\"-952\"><name x=\"-1338\" y=\"-982\">s2</name></location><location id=\"id1\" x=\"-1064\" y=\"-800\"><name x=\"-1074\" y=\"-830\">s4</name><urgent/></location><location id=\"id2\" x=\"-1328\" y=\"-696\"><name x=\"-1338\" y=\"-726\">s3</name><label kind=\"invariant\" x=\"-1568\" y=\"-720\">timeU[r2][r1] == INFINITE_TIME\n|| c&lt;=timeU[r2][r1]</label></location><location id=\"id3\" x=\"-1328\" y=\"-840\"><name x=\"-1352\" y=\"-864\">s1</name><urgent/></location><init ref=\"id3\"/>" + (!r.get(Model.Properties.R1_IS_DOWNSTREAM).as(Boolean.class)?"<transition><source ref=\"id0\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1592\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1592\" y=\"-1000\">c:=0</label><nail x=\"-1360\" y=\"-984\"/><nail x=\"-1616\" y=\"-984\"/><nail x=\"-1616\" y=\"-512\"/><nail x=\"-784\" y=\"-512\"/><nail x=\"-784\" y=\"-736\"/></transition>":"") + "<transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1184\" y=\"-784\">r2_reacting?</label><nail x=\"-1248\" y=\"-768\"/><nail x=\"-1096\" y=\"-768\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1096\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; output_reactant+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1096\" y=\"-608\">output_reacting!</label><label kind=\"assignment\" x=\"-1096\" y=\"-592\">output_reactant:=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + ",\nc:=0</label><nail x=\"-1280\" y=\"-656\"/><nail x=\"-1104\" y=\"-656\"/><nail x=\"-1104\" y=\"-560\"/><nail x=\"-848\" y=\"-560\"/><nail x=\"-848\" y=\"-704\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1576\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; output_reactant+delta&lt;0</label><label kind=\"synchronisation\" x=\"-1576\" y=\"-608\">output_reacting!</label><label kind=\"assignment\" x=\"-1576\" y=\"-592\">output_reactant:=0,\nc:=0</label><nail x=\"-1416\" y=\"-656\"/><nail x=\"-1584\" y=\"-656\"/><nail x=\"-1584\" y=\"-544\"/><nail x=\"-816\" y=\"-544\"/><nail x=\"-816\" y=\"-720\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1224\" y=\"-768\">r1_reacting?</label><nail x=\"-1248\" y=\"-752\"/><nail x=\"-1088\" y=\"-752\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1384\" y=\"-648\">c&gt;=timeL[r2][r1]\n&amp;&amp; output_reactant+delta&gt;=0\n&amp;&amp; output_reactant+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1384\" y=\"-608\">output_reacting!</label><label kind=\"assignment\" x=\"-1384\" y=\"-592\">output_reactant:=output_reactant+delta,\nc:=0</label><nail x=\"-1328\" y=\"-656\"/><nail x=\"-1392\" y=\"-656\"/><nail x=\"-1392\" y=\"-552\"/><nail x=\"-832\" y=\"-552\"/><nail x=\"-832\" y=\"-712\"/></transition>" + (!r.get(Model.Properties.R2_IS_DOWNSTREAM).as(Boolean.class)?"<transition><source ref=\"id0\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1592\" y=\"-984\">r2_reacting?</label><label kind=\"assignment\" x=\"-1592\" y=\"-968\">c:=0</label><nail x=\"-1600\" y=\"-952\"/><nail x=\"-1600\" y=\"-528\"/><nail x=\"-800\" y=\"-528\"/><nail x=\"-800\" y=\"-728\"/></transition>":"") + "<transition><source ref=\"id1\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"-1272\" y=\"-968\">timeL[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-952\" y=\"-800\"/><nail x=\"-952\" y=\"-952\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"-1480\" y=\"-912\">timeL[reactant2][reactant1] == INFINITE_TIME</label></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1296\" y=\"-840\">timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1296\" y=\"-816\">c:=timeU[reactant2][reactant1],\nr1:=reactant1,\nr2:=reactant2</label><nail x=\"-1248\" y=\"-800\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1272\" y=\"-752\">(timeU[reactant2][reactant1] == INFINITE_TIME\n&amp;&amp; timeL[reactant2][reactant1] != INFINITE_TIME)\n|| (timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant2][reactant1])</label><label kind=\"assignment\" x=\"-1272\" y=\"-704\">r1:=reactant1,\nr2:=reactant2</label><nail x=\"-1064\" y=\"-696\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1528\" y=\"-824\">timeL[reactant2][reactant1] \n  != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1448\" y=\"-792\">r1 := reactant1,\nr2 := reactant2,\nc:=0</label></transition></template>").getBytes()));
				//document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output, const int timeL[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r1, r2;</declaration><location id=\"id5\" x=\"-1624\" y=\"-704\"><name x=\"-1656\" y=\"-736\">stubborn</name><urgent/></location><location id=\"id6\" x=\"-1384\" y=\"-976\"><name x=\"-1392\" y=\"-1008\">not_reacting</name></location><location id=\"id7\" x=\"-960\" y=\"-856\"><name x=\"-952\" y=\"-880\">resetting</name><urgent/></location><location id=\"id8\" x=\"-1384\" y=\"-768\"><name x=\"-1416\" y=\"-808\">reacting</name><label kind=\"invariant\" x=\"-1624\" y=\"-792\">timeU[r2][r1] == INFINITE_TIME\n|| c&lt;=timeU[r2][r1]</label></location><location id=\"id9\" x=\"-1384\" y=\"-896\"><name x=\"-1424\" y=\"-920\">start</name><urgent/></location><location id=\"id10\" x=\"-1224\" y=\"-632\"><name x=\"-1224\" y=\"-616\">about_to_react</name></location><init ref=\"id9\"/>" + (!r.get(Model.Properties.R2_IS_DOWNSTREAM).as(Boolean.class)?"<transition><source ref=\"id6\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-992\">r2_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-992\">c:=0</label><nail x=\"-1664\" y=\"-976\"/><nail x=\"-1664\" y=\"-584\"/><nail x=\"-920\" y=\"-584\"/><nail x=\"-920\" y=\"-784\"/></transition>":"") + "<transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1600\" y=\"-688\">c&gt;=timeL[r2][r1]</label><label kind=\"synchronisation\" x=\"-1600\" y=\"-672\">r2_reacting?</label><nail x=\"-1488\" y=\"-656\"/><nail x=\"-1608\" y=\"-656\"/></transition><transition><source ref=\"id5\"/><target ref=\"id8\"/><nail x=\"-1608\" y=\"-744\"/><nail x=\"-1512\" y=\"-744\"/></transition><transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1584\" y=\"-736\">c&gt;=timeL[r2][r1]</label><label kind=\"synchronisation\" x=\"-1584\" y=\"-720\">r1_reacting?</label><nail x=\"-1464\" y=\"-704\"/></transition><transition><source ref=\"id10\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1248\" y=\"-680\">output_reacting?</label><label kind=\"assignment\" x=\"-1248\" y=\"-664\">c := timeU[r2][r1]</label><nail x=\"-1112\" y=\"-632\"/><nail x=\"-1112\" y=\"-688\"/><nail x=\"-1344\" y=\"-688\"/></transition>" + (!r.get(Model.Properties.R1_IS_DOWNSTREAM).as(Boolean.class)?"<transition><source ref=\"id6\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-1016\">c:=0</label><nail x=\"-1416\" y=\"-1000\"/><nail x=\"-1672\" y=\"-1000\"/><nail x=\"-1672\" y=\"-576\"/><nail x=\"-912\" y=\"-576\"/><nail x=\"-912\" y=\"-792\"/></transition>":"") + "<transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1320\" y=\"-816\">c&lt;timeL[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1088\" y=\"-816\">r2_reacting?</label><nail x=\"-1328\" y=\"-800\"/><nail x=\"-992\" y=\"-800\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1320\" y=\"-840\">c&lt;timeL[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1088\" y=\"-840\">r1_reacting?</label><nail x=\"-1328\" y=\"-824\"/><nail x=\"-984\" y=\"-824\"/></transition><transition><source ref=\"id8\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-1448\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><nail x=\"-1384\" y=\"-648\"/><nail x=\"-1456\" y=\"-648\"/><nail x=\"-1456\" y=\"-600\"/><nail x=\"-1312\" y=\"-600\"/></transition><transition><source ref=\"id8\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-1632\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&lt;0</label><nail x=\"-1472\" y=\"-648\"/><nail x=\"-1640\" y=\"-648\"/><nail x=\"-1640\" y=\"-592\"/><nail x=\"-1312\" y=\"-592\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1096\" y=\"-680\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&gt;=0\n&amp;&amp; output+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1096\" y=\"-640\">output_reacting!</label><label kind=\"assignment\" x=\"-1096\" y=\"-624\">output:=output+delta,\nc:=0</label><nail x=\"-1328\" y=\"-696\"/><nail x=\"-1104\" y=\"-696\"/><nail x=\"-1104\" y=\"-592\"/><nail x=\"-928\" y=\"-592\"/><nail x=\"-928\" y=\"-776\"/></transition><transition><source ref=\"id7\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1328\" y=\"-992\">timeL[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-960\" y=\"-976\"/></transition><transition><source ref=\"id9\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1536\" y=\"-952\">timeL[reactant2][reactant1] == INFINITE_TIME</label></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1320\" y=\"-896\">timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1320\" y=\"-872\">c:=timeU[reactant2][reactant1],\nr1:=reactant1,r2:=reactant2</label><nail x=\"-1328\" y=\"-856\"/></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1320\" y=\"-792\">(timeU[reactant2][reactant1] == INFINITE_TIME\n&amp;&amp; timeL[reactant2][reactant1] != INFINITE_TIME)\n|| (timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant2][reactant1])</label><label kind=\"assignment\" x=\"-1320\" y=\"-736\">r1:=reactant1, r2:=reactant2</label><nail x=\"-960\" y=\"-720\"/><nail x=\"-1328\" y=\"-720\"/></transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1584\" y=\"-880\">timeL[reactant2][reactant1]\n   != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1504\" y=\"-848\">r1 := reactant1,\nr2 := reactant2,\nc := 0</label></transition></template>").getBytes()));
				//Quello qui sopra toglieva le transizioni di ritorno da "not_reacting" a "resetting" se erano di un reagente downstream. Quello qui sotto le fa lo stesso
				//document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + ((r.get(OUTPUT_REACTANT).as(String.class).equals(r.get(REACTANT).as(String.class))) ? "" : "_" + r.get(OUTPUT_REACTANT).as(String.class)) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output, const int timeL[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r1, r2;</declaration><location id=\"id5\" x=\"-1624\" y=\"-704\"><name x=\"-1656\" y=\"-736\">stubborn</name><urgent/></location><location id=\"id6\" x=\"-1384\" y=\"-976\"><name x=\"-1392\" y=\"-1008\">not_reacting</name></location><location id=\"id7\" x=\"-960\" y=\"-856\"><name x=\"-952\" y=\"-880\">resetting</name><urgent/></location><location id=\"id8\" x=\"-1384\" y=\"-768\"><name x=\"-1416\" y=\"-808\">reacting</name><label kind=\"invariant\" x=\"-1624\" y=\"-792\">timeU[r2][r1] == INFINITE_TIME\n|| c&lt;=timeU[r2][r1]</label></location><location id=\"id9\" x=\"-1384\" y=\"-896\"><name x=\"-1424\" y=\"-920\">start</name><urgent/></location><location id=\"id10\" x=\"-1224\" y=\"-632\"><name x=\"-1224\" y=\"-616\">about_to_react</name></location><init ref=\"id9\"/><transition><source ref=\"id6\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-992\">r2_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-992\">c:=0</label><nail x=\"-1664\" y=\"-976\"/><nail x=\"-1664\" y=\"-584\"/><nail x=\"-920\" y=\"-584\"/><nail x=\"-920\" y=\"-784\"/></transition><transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1600\" y=\"-688\">c&gt;=timeL[r2][r1]</label><label kind=\"synchronisation\" x=\"-1600\" y=\"-672\">r2_reacting?</label><nail x=\"-1488\" y=\"-656\"/><nail x=\"-1608\" y=\"-656\"/></transition><transition><source ref=\"id5\"/><target ref=\"id8\"/><nail x=\"-1608\" y=\"-744\"/><nail x=\"-1512\" y=\"-744\"/></transition><transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1584\" y=\"-736\">c&gt;=timeL[r2][r1]</label><label kind=\"synchronisation\" x=\"-1584\" y=\"-720\">r1_reacting?</label><nail x=\"-1464\" y=\"-704\"/></transition><transition><source ref=\"id10\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1248\" y=\"-680\">output_reacting?</label><label kind=\"assignment\" x=\"-1248\" y=\"-664\">c := timeU[r2][r1]</label><nail x=\"-1112\" y=\"-632\"/><nail x=\"-1112\" y=\"-688\"/><nail x=\"-1344\" y=\"-688\"/></transition><transition><source ref=\"id6\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-1016\">c:=0</label><nail x=\"-1416\" y=\"-1000\"/><nail x=\"-1672\" y=\"-1000\"/><nail x=\"-1672\" y=\"-576\"/><nail x=\"-912\" y=\"-576\"/><nail x=\"-912\" y=\"-792\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1320\" y=\"-816\">c&lt;timeL[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1088\" y=\"-816\">r2_reacting?</label><nail x=\"-1328\" y=\"-800\"/><nail x=\"-992\" y=\"-800\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1320\" y=\"-840\">c&lt;timeL[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1088\" y=\"-840\">r1_reacting?</label><nail x=\"-1328\" y=\"-824\"/><nail x=\"-984\" y=\"-824\"/></transition><transition><source ref=\"id8\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-1448\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><nail x=\"-1384\" y=\"-648\"/><nail x=\"-1456\" y=\"-648\"/><nail x=\"-1456\" y=\"-600\"/><nail x=\"-1312\" y=\"-600\"/></transition><transition><source ref=\"id8\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-1632\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&lt;0</label><nail x=\"-1472\" y=\"-648\"/><nail x=\"-1640\" y=\"-648\"/><nail x=\"-1640\" y=\"-592\"/><nail x=\"-1312\" y=\"-592\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1096\" y=\"-680\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&gt;=0\n&amp;&amp; output+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1096\" y=\"-640\">output_reacting!</label><label kind=\"assignment\" x=\"-1096\" y=\"-624\">output:=output+delta,\nc:=0</label><nail x=\"-1328\" y=\"-696\"/><nail x=\"-1104\" y=\"-696\"/><nail x=\"-1104\" y=\"-592\"/><nail x=\"-928\" y=\"-592\"/><nail x=\"-928\" y=\"-776\"/></transition><transition><source ref=\"id7\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1328\" y=\"-992\">timeL[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-960\" y=\"-976\"/></transition><transition><source ref=\"id9\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1536\" y=\"-952\">timeL[reactant2][reactant1] == INFINITE_TIME</label></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1320\" y=\"-896\">timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1320\" y=\"-872\">c:=timeU[reactant2][reactant1],\nr1:=reactant1,r2:=reactant2</label><nail x=\"-1328\" y=\"-856\"/></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1320\" y=\"-792\">(timeU[reactant2][reactant1] == INFINITE_TIME\n&amp;&amp; timeL[reactant2][reactant1] != INFINITE_TIME)\n|| (timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant2][reactant1])</label><label kind=\"assignment\" x=\"-1320\" y=\"-736\">r1:=reactant1, r2:=reactant2</label><nail x=\"-960\" y=\"-720\"/><nail x=\"-1328\" y=\"-720\"/></transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1584\" y=\"-880\">timeL[reactant2][reactant1]\n   != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1504\" y=\"-848\">r1 := reactant1,\nr2 := reactant2,\nc := 0</label></transition></template>").getBytes()));
				//Quello qui sotto mette le locazioni start, stubborn e resetting a committed invece che a urgent (a occhio parrebbe servire a renderlo meno non-deterministico, ma forse non tanto..?)
				if (r.get(CATALYST).as(String.class).equals(r.get(REACTANT).as(String.class)) && r.get(SCENARIO).as(Integer.class) != 0) { //R1 == R2: use only r1_reacting to avoid input nondeterminism
					document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + ((r.get(OUTPUT_REACTANT).as(String.class).equals(r.get(REACTANT).as(String.class))) ? "" : "_" + r.get(OUTPUT_REACTANT).as(String.class)) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output, const int timeL[" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r1, r2;\nint[-1, 1073741822] T;</declaration><location id=\"id0\" x=\"-1512\" y=\"-696\"><name x=\"-1584\" y=\"-688\">stubborn</name><committed/></location><location id=\"id1\" x=\"-1384\" y=\"-976\"><name x=\"-1392\" y=\"-1008\">not_reacting</name></location><location id=\"id2\" x=\"-960\" y=\"-856\"><name x=\"-952\" y=\"-880\">resetting</name><committed/></location><location id=\"id3\" x=\"-1384\" y=\"-768\"><name x=\"-1416\" y=\"-808\">reacting</name><label kind=\"invariant\" x=\"-1624\" y=\"-792\">timeU[r2][r1] == INFINITE_TIME\n|| c&lt;=timeU[r2][r1]</label></location><location id=\"id4\" x=\"-1384\" y=\"-896\"><name x=\"-1424\" y=\"-920\">start</name><committed/></location><location id=\"id5\" x=\"-1248\" y=\"-624\"><name x=\"-1360\" y=\"-616\">about_to_react</name></location><init ref=\"id4\"/><transition><source ref=\"id0\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1624\" y=\"-656\">c&lt;timeL[r2][r1]</label><nail x=\"-1512\" y=\"-592\"/><nail x=\"-928\" y=\"-592\"/><nail x=\"-928\" y=\"-776\"/></transition><transition><source ref=\"id0\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1632\" y=\"-728\">c&gt;=timeL[r2][r1]</label><nail x=\"-1512\" y=\"-744\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1496\" y=\"-736\">r1_reacting?</label><nail x=\"-1408\" y=\"-720\"/><nail x=\"-1488\" y=\"-720\"/></transition><transition><source ref=\"id5\"/><target ref=\"id3\"/><label kind=\"synchronisation\" x=\"-1272\" y=\"-672\">output_reacting?</label><label kind=\"assignment\" x=\"-1272\" y=\"-656\">c := timeU[r2][r1]</label><nail x=\"-1144\" y=\"-624\"/><nail x=\"-1144\" y=\"-680\"/><nail x=\"-1344\" y=\"-680\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-1016\">c:=0</label><nail x=\"-1416\" y=\"-1000\"/><nail x=\"-1672\" y=\"-1000\"/><nail x=\"-1672\" y=\"-576\"/><nail x=\"-912\" y=\"-576\"/><nail x=\"-912\" y=\"-792\"/></transition><transition><source ref=\"id3\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1448\" y=\"-672\">c&gt;=timeL[r2][r1]\n&amp;&amp; (output+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "\n|| output+delta&lt;0)</label><nail x=\"-1384\" y=\"-680\"/><nail x=\"-1456\" y=\"-680\"/><nail x=\"-1456\" y=\"-624\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1128\" y=\"-736\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&gt;=0\n&amp;&amp; output+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1128\" y=\"-696\">output_reacting!</label><label kind=\"assignment\" x=\"-1128\" y=\"-680\">output:=output+delta,\nc:=0</label><nail x=\"-1320\" y=\"-696\"/><nail x=\"-936\" y=\"-696\"/><nail x=\"-936\" y=\"-768\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1328\" y=\"-992\">timeL[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-960\" y=\"-976\"/></transition><transition><source ref=\"id4\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1536\" y=\"-952\">timeL[reactant2][reactant1] == INFINITE_TIME</label></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-896\">timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1320\" y=\"-872\">c:=timeU[reactant2][reactant1],\nr1:=reactant1,r2:=reactant2,\nT:=timeL[r2][r1]</label><nail x=\"-1328\" y=\"-856\"/></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-824\">(timeU[reactant2][reactant1] == INFINITE_TIME\n&amp;&amp; timeL[reactant2][reactant1] != INFINITE_TIME)\n|| (timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant2][reactant1])</label><label kind=\"assignment\" x=\"-1320\" y=\"-768\">r1:=reactant1,r2:=reactant2,\nT:=timeL[r2][r1]</label><nail x=\"-960\" y=\"-752\"/><nail x=\"-1328\" y=\"-752\"/></transition><transition><source ref=\"id4\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1584\" y=\"-880\">timeL[reactant2][reactant1]\n   != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1504\" y=\"-848\">r1 := reactant1,\nr2 := reactant2,\nT := timeL[r2][r1],\nc := 0</label></transition></template>").getBytes()));
				} else if (r.get(SCENARIO).as(Integer.class) == 0) { //If the scenario is 0, we depend only on reactant1, so we don't need to refer to reactant2 (both as in using a monodimensional table and not receiving input on the r2_reacting channel)
					document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + ((r.get(OUTPUT_REACTANT).as(String.class).equals(r.get(REACTANT).as(String.class))) ? "" : "_" + r.get(OUTPUT_REACTANT).as(String.class)) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output, const int timeL[" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r1, r2;\nint[-1, 1073741822] T;</declaration><location id=\"id0\" x=\"-1512\" y=\"-696\"><name x=\"-1584\" y=\"-688\">stubborn</name><committed/></location><location id=\"id1\" x=\"-1384\" y=\"-976\"><name x=\"-1392\" y=\"-1008\">not_reacting</name></location><location id=\"id2\" x=\"-960\" y=\"-856\"><name x=\"-952\" y=\"-880\">resetting</name><committed/></location><location id=\"id3\" x=\"-1384\" y=\"-768\"><name x=\"-1416\" y=\"-808\">reacting</name><label kind=\"invariant\" x=\"-1624\" y=\"-792\">timeU[r1] == INFINITE_TIME\n|| c&lt;=timeU[r1]</label></location><location id=\"id4\" x=\"-1384\" y=\"-896\"><name x=\"-1424\" y=\"-920\">start</name><committed/></location><location id=\"id5\" x=\"-1248\" y=\"-624\"><name x=\"-1360\" y=\"-616\">about_to_react</name></location><init ref=\"id4\"/><transition><source ref=\"id0\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1624\" y=\"-656\">c&lt;timeL[r1]</label><nail x=\"-1512\" y=\"-592\"/><nail x=\"-928\" y=\"-592\"/><nail x=\"-928\" y=\"-776\"/></transition><transition><source ref=\"id0\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1632\" y=\"-728\">c&gt;=timeL[r1]</label><nail x=\"-1512\" y=\"-744\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1496\" y=\"-736\">r1_reacting?</label><nail x=\"-1408\" y=\"-720\"/><nail x=\"-1488\" y=\"-720\"/></transition><transition><source ref=\"id5\"/><target ref=\"id3\"/><label kind=\"synchronisation\" x=\"-1272\" y=\"-672\">output_reacting?</label><label kind=\"assignment\" x=\"-1272\" y=\"-656\">c := timeU[r1]</label><nail x=\"-1144\" y=\"-624\"/><nail x=\"-1144\" y=\"-680\"/><nail x=\"-1344\" y=\"-680\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-1016\">c:=0</label><nail x=\"-1416\" y=\"-1000\"/><nail x=\"-1672\" y=\"-1000\"/><nail x=\"-1672\" y=\"-576\"/><nail x=\"-912\" y=\"-576\"/><nail x=\"-912\" y=\"-792\"/></transition><transition><source ref=\"id3\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1448\" y=\"-672\">c&gt;=timeL[r1]\n&amp;&amp; (output+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "\n|| output+delta&lt;0)</label><nail x=\"-1384\" y=\"-680\"/><nail x=\"-1456\" y=\"-680\"/><nail x=\"-1456\" y=\"-624\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1128\" y=\"-736\">c&gt;=timeL[r1]\n&amp;&amp; output+delta&gt;=0\n&amp;&amp; output+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1128\" y=\"-696\">output_reacting!</label><label kind=\"assignment\" x=\"-1128\" y=\"-680\">output:=output+delta,\nc:=0</label><nail x=\"-1320\" y=\"-696\"/><nail x=\"-936\" y=\"-696\"/><nail x=\"-936\" y=\"-768\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1328\" y=\"-992\">timeL[reactant1] == INFINITE_TIME</label><nail x=\"-960\" y=\"-976\"/></transition><transition><source ref=\"id4\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1536\" y=\"-952\">timeL[reactant1] == INFINITE_TIME</label></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-896\">timeU[reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant1]</label><label kind=\"assignment\" x=\"-1320\" y=\"-872\">c:=timeU[reactant1],\nr1:=reactant1,T:=timeL[r1]</label><nail x=\"-1328\" y=\"-856\"/></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-824\">(timeU[reactant1] == INFINITE_TIME\n&amp;&amp; timeL[reactant1] != INFINITE_TIME)\n|| (timeU[reactant1] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant1])</label><label kind=\"assignment\" x=\"-1320\" y=\"-768\">r1:=reactant1,T:=timeL[r1]</label><nail x=\"-960\" y=\"-752\"/><nail x=\"-1328\" y=\"-752\"/></transition><transition><source ref=\"id4\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1584\" y=\"-880\">timeL[reactant1]\n   != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1504\" y=\"-848\">r1 := reactant1,\nT := timeL[r1],\nc := 0</label></transition></template>").getBytes()));
					//document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + ((r.get(OUTPUT_REACTANT).as(String.class).equals(r.get(REACTANT).as(String.class))) ? "" : "_" + r.get(OUTPUT_REACTANT).as(String.class)) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output, const int timeL[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r1, r2;</declaration><location id=\"id5\" x=\"-1624\" y=\"-704\"><name x=\"-1656\" y=\"-736\">stubborn</name><committed/></location><location id=\"id6\" x=\"-1384\" y=\"-976\"><name x=\"-1392\" y=\"-1008\">not_reacting</name></location><location id=\"id7\" x=\"-960\" y=\"-856\"><name x=\"-952\" y=\"-880\">resetting</name><committed/></location><location id=\"id8\" x=\"-1384\" y=\"-768\"><name x=\"-1416\" y=\"-808\">reacting</name><label kind=\"invariant\" x=\"-1624\" y=\"-792\">timeU[r2][r1] == INFINITE_TIME\n|| c&lt;=timeU[r2][r1]</label></location><location id=\"id9\" x=\"-1384\" y=\"-896\"><name x=\"-1424\" y=\"-920\">start</name><committed/></location><location id=\"id10\" x=\"-1224\" y=\"-632\"><name x=\"-1224\" y=\"-616\">about_to_react</name></location><init ref=\"id9\"/><transition><source ref=\"id5\"/><target ref=\"id8\"/><nail x=\"-1608\" y=\"-744\"/><nail x=\"-1512\" y=\"-744\"/></transition><transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1584\" y=\"-736\">c&gt;=timeL[r2][r1]</label><label kind=\"synchronisation\" x=\"-1584\" y=\"-720\">r1_reacting?</label><nail x=\"-1464\" y=\"-704\"/></transition><transition><source ref=\"id10\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1248\" y=\"-680\">output_reacting?</label><label kind=\"assignment\" x=\"-1248\" y=\"-664\">c := timeU[r2][r1]</label><nail x=\"-1112\" y=\"-632\"/><nail x=\"-1112\" y=\"-688\"/><nail x=\"-1344\" y=\"-688\"/></transition><transition><source ref=\"id6\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-1016\">c:=0</label><nail x=\"-1416\" y=\"-1000\"/><nail x=\"-1672\" y=\"-1000\"/><nail x=\"-1672\" y=\"-576\"/><nail x=\"-912\" y=\"-576\"/><nail x=\"-912\" y=\"-792\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1320\" y=\"-840\">c&lt;timeL[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1088\" y=\"-840\">r1_reacting?</label><nail x=\"-1328\" y=\"-824\"/><nail x=\"-984\" y=\"-824\"/></transition><transition><source ref=\"id8\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-1448\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><nail x=\"-1384\" y=\"-648\"/><nail x=\"-1456\" y=\"-648\"/><nail x=\"-1456\" y=\"-600\"/><nail x=\"-1312\" y=\"-600\"/></transition><transition><source ref=\"id8\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-1632\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&lt;0</label><nail x=\"-1472\" y=\"-648\"/><nail x=\"-1640\" y=\"-648\"/><nail x=\"-1640\" y=\"-592\"/><nail x=\"-1312\" y=\"-592\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1096\" y=\"-680\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&gt;=0\n&amp;&amp; output+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1096\" y=\"-640\">output_reacting!</label><label kind=\"assignment\" x=\"-1096\" y=\"-624\">output:=output+delta,\nc:=0</label><nail x=\"-1328\" y=\"-696\"/><nail x=\"-1104\" y=\"-696\"/><nail x=\"-1104\" y=\"-592\"/><nail x=\"-928\" y=\"-592\"/><nail x=\"-928\" y=\"-776\"/></transition><transition><source ref=\"id7\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1328\" y=\"-992\">timeL[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-960\" y=\"-976\"/></transition><transition><source ref=\"id9\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1536\" y=\"-952\">timeL[reactant2][reactant1] == INFINITE_TIME</label></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1320\" y=\"-896\">timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1320\" y=\"-872\">c:=timeU[reactant2][reactant1],\nr1:=reactant1,r2:=reactant2</label><nail x=\"-1328\" y=\"-856\"/></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1320\" y=\"-792\">(timeU[reactant2][reactant1] == INFINITE_TIME\n&amp;&amp; timeL[reactant2][reactant1] != INFINITE_TIME)\n|| (timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant2][reactant1])</label><label kind=\"assignment\" x=\"-1320\" y=\"-736\">r1:=reactant1, r2:=reactant2</label><nail x=\"-960\" y=\"-720\"/><nail x=\"-1328\" y=\"-720\"/></transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1584\" y=\"-880\">timeL[reactant2][reactant1]\n   != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1504\" y=\"-848\">r1 := reactant1,\nr2 := reactant2,\nc := 0</label></transition></template>").getBytes()));
				} else { //R1 != R2 and no unidimensional scenario (scen. 0): use both r1_reacting and r2_reacting
					document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + ((r.get(OUTPUT_REACTANT).as(String.class).equals(r.get(REACTANT).as(String.class))) ? "" : "_" + r.get(OUTPUT_REACTANT).as(String.class)) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output, const int timeL[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r1, r2;\nint[-1, 1073741822] T;</declaration><location id=\"id0\" x=\"-1512\" y=\"-696\"><name x=\"-1584\" y=\"-688\">stubborn</name><committed/></location><location id=\"id1\" x=\"-1384\" y=\"-976\"><name x=\"-1392\" y=\"-1008\">not_reacting</name></location><location id=\"id2\" x=\"-960\" y=\"-856\"><name x=\"-952\" y=\"-880\">resetting</name><committed/></location><location id=\"id3\" x=\"-1384\" y=\"-768\"><name x=\"-1416\" y=\"-808\">reacting</name><label kind=\"invariant\" x=\"-1624\" y=\"-792\">timeU[r2][r1] == INFINITE_TIME\n|| c&lt;=timeU[r2][r1]</label></location><location id=\"id4\" x=\"-1384\" y=\"-896\"><name x=\"-1424\" y=\"-920\">start</name><committed/></location><location id=\"id5\" x=\"-1248\" y=\"-624\"><name x=\"-1360\" y=\"-616\">about_to_react</name></location><init ref=\"id4\"/><transition><source ref=\"id0\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1624\" y=\"-656\">c&lt;timeL[r2][r1]</label><nail x=\"-1512\" y=\"-592\"/><nail x=\"-928\" y=\"-592\"/><nail x=\"-928\" y=\"-776\"/></transition><transition><source ref=\"id0\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1632\" y=\"-728\">c&gt;=timeL[r2][r1]</label><nail x=\"-1512\" y=\"-744\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-992\">r2_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-992\">c:=0</label><nail x=\"-1664\" y=\"-976\"/><nail x=\"-1664\" y=\"-584\"/><nail x=\"-920\" y=\"-584\"/><nail x=\"-920\" y=\"-784\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1488\" y=\"-712\">r2_reacting?</label><nail x=\"-1400\" y=\"-696\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1496\" y=\"-736\">r1_reacting?</label><nail x=\"-1408\" y=\"-720\"/><nail x=\"-1488\" y=\"-720\"/></transition><transition><source ref=\"id5\"/><target ref=\"id3\"/><label kind=\"synchronisation\" x=\"-1272\" y=\"-672\">output_reacting?</label><label kind=\"assignment\" x=\"-1272\" y=\"-656\">c := timeU[r2][r1]</label><nail x=\"-1144\" y=\"-624\"/><nail x=\"-1144\" y=\"-680\"/><nail x=\"-1344\" y=\"-680\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-1016\">c:=0</label><nail x=\"-1416\" y=\"-1000\"/><nail x=\"-1672\" y=\"-1000\"/><nail x=\"-1672\" y=\"-576\"/><nail x=\"-912\" y=\"-576\"/><nail x=\"-912\" y=\"-792\"/></transition><transition><source ref=\"id3\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1448\" y=\"-672\">c&gt;=timeL[r2][r1]\n&amp;&amp; (output+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "\n|| output+delta&lt;0)</label><nail x=\"-1384\" y=\"-680\"/><nail x=\"-1456\" y=\"-680\"/><nail x=\"-1456\" y=\"-624\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1128\" y=\"-736\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&gt;=0\n&amp;&amp; output+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1128\" y=\"-696\">output_reacting!</label><label kind=\"assignment\" x=\"-1128\" y=\"-680\">output:=output+delta,\nc:=0</label><nail x=\"-1320\" y=\"-696\"/><nail x=\"-936\" y=\"-696\"/><nail x=\"-936\" y=\"-768\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1328\" y=\"-992\">timeL[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-960\" y=\"-976\"/></transition><transition><source ref=\"id4\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1536\" y=\"-952\">timeL[reactant2][reactant1] == INFINITE_TIME</label></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-896\">timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1320\" y=\"-872\">c:=timeU[reactant2][reactant1],\nr1:=reactant1, r2:=reactant2, T:=timeL[r2][r1]</label><nail x=\"-1328\" y=\"-856\"/></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1320\" y=\"-824\">(timeU[reactant2][reactant1] == INFINITE_TIME\n&amp;&amp; timeL[reactant2][reactant1] != INFINITE_TIME)\n|| (timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant2][reactant1])</label><label kind=\"assignment\" x=\"-1320\" y=\"-768\">r1:=reactant1, r2:=reactant2, T:=timeL[r2][r1]</label><nail x=\"-960\" y=\"-752\"/><nail x=\"-1328\" y=\"-752\"/></transition><transition><source ref=\"id4\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1584\" y=\"-880\">timeL[reactant2][reactant1]\n   != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1504\" y=\"-848\">r1 := reactant1,\nr2 := reactant2,\nT := timeL[r2][r1],\nc := 0</label></transition></template>").getBytes()));
					//document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + ((r.get(OUTPUT_REACTANT).as(String.class).equals(r.get(REACTANT).as(String.class))) ? "" : "_" + r.get(OUTPUT_REACTANT).as(String.class)) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, int &amp;output, const int timeL[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting, broadcast chan &amp;output_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r1, r2;</declaration><location id=\"id5\" x=\"-1624\" y=\"-704\"><name x=\"-1656\" y=\"-736\">stubborn</name><committed/></location><location id=\"id6\" x=\"-1384\" y=\"-976\"><name x=\"-1392\" y=\"-1008\">not_reacting</name></location><location id=\"id7\" x=\"-960\" y=\"-856\"><name x=\"-952\" y=\"-880\">resetting</name><committed/></location><location id=\"id8\" x=\"-1384\" y=\"-768\"><name x=\"-1416\" y=\"-808\">reacting</name><label kind=\"invariant\" x=\"-1624\" y=\"-792\">timeU[r2][r1] == INFINITE_TIME\n|| c&lt;=timeU[r2][r1]</label></location><location id=\"id9\" x=\"-1384\" y=\"-896\"><name x=\"-1424\" y=\"-920\">start</name><committed/></location><location id=\"id10\" x=\"-1224\" y=\"-632\"><name x=\"-1224\" y=\"-616\">about_to_react</name></location><init ref=\"id9\"/><transition><source ref=\"id6\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-992\">r2_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-992\">c:=0</label><nail x=\"-1664\" y=\"-976\"/><nail x=\"-1664\" y=\"-584\"/><nail x=\"-920\" y=\"-584\"/><nail x=\"-920\" y=\"-784\"/></transition><transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1600\" y=\"-688\">c&gt;=timeL[r2][r1]</label><label kind=\"synchronisation\" x=\"-1600\" y=\"-672\">r2_reacting?</label><nail x=\"-1488\" y=\"-656\"/><nail x=\"-1608\" y=\"-656\"/></transition><transition><source ref=\"id5\"/><target ref=\"id8\"/><nail x=\"-1608\" y=\"-744\"/><nail x=\"-1512\" y=\"-744\"/></transition><transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1584\" y=\"-736\">c&gt;=timeL[r2][r1]</label><label kind=\"synchronisation\" x=\"-1584\" y=\"-720\">r1_reacting?</label><nail x=\"-1464\" y=\"-704\"/></transition><transition><source ref=\"id10\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1248\" y=\"-680\">output_reacting?</label><label kind=\"assignment\" x=\"-1248\" y=\"-664\">c := timeU[r2][r1]</label><nail x=\"-1112\" y=\"-632\"/><nail x=\"-1112\" y=\"-688\"/><nail x=\"-1344\" y=\"-688\"/></transition><transition><source ref=\"id6\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1536\" y=\"-1016\">c:=0</label><nail x=\"-1416\" y=\"-1000\"/><nail x=\"-1672\" y=\"-1000\"/><nail x=\"-1672\" y=\"-576\"/><nail x=\"-912\" y=\"-576\"/><nail x=\"-912\" y=\"-792\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1320\" y=\"-816\">c&lt;timeL[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1088\" y=\"-816\">r2_reacting?</label><nail x=\"-1328\" y=\"-800\"/><nail x=\"-992\" y=\"-800\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1320\" y=\"-840\">c&lt;timeL[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1088\" y=\"-840\">r1_reacting?</label><nail x=\"-1328\" y=\"-824\"/><nail x=\"-984\" y=\"-824\"/></transition><transition><source ref=\"id8\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-1448\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&gt;" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><nail x=\"-1384\" y=\"-648\"/><nail x=\"-1456\" y=\"-648\"/><nail x=\"-1456\" y=\"-600\"/><nail x=\"-1312\" y=\"-600\"/></transition><transition><source ref=\"id8\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-1632\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&lt;0</label><nail x=\"-1472\" y=\"-648\"/><nail x=\"-1640\" y=\"-648\"/><nail x=\"-1640\" y=\"-592\"/><nail x=\"-1312\" y=\"-592\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1096\" y=\"-680\">c&gt;=timeL[r2][r1]\n&amp;&amp; output+delta&gt;=0\n&amp;&amp; output+delta&lt;=" + m.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1096\" y=\"-640\">output_reacting!</label><label kind=\"assignment\" x=\"-1096\" y=\"-624\">output:=output+delta,\nc:=0</label><nail x=\"-1328\" y=\"-696\"/><nail x=\"-1104\" y=\"-696\"/><nail x=\"-1104\" y=\"-592\"/><nail x=\"-928\" y=\"-592\"/><nail x=\"-928\" y=\"-776\"/></transition><transition><source ref=\"id7\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1328\" y=\"-992\">timeL[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-960\" y=\"-976\"/></transition><transition><source ref=\"id9\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1536\" y=\"-952\">timeL[reactant2][reactant1] == INFINITE_TIME</label></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1320\" y=\"-896\">timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1320\" y=\"-872\">c:=timeU[reactant2][reactant1],\nr1:=reactant1,r2:=reactant2</label><nail x=\"-1328\" y=\"-856\"/></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1320\" y=\"-792\">(timeU[reactant2][reactant1] == INFINITE_TIME\n&amp;&amp; timeL[reactant2][reactant1] != INFINITE_TIME)\n|| (timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant2][reactant1])</label><label kind=\"assignment\" x=\"-1320\" y=\"-736\">r1:=reactant1, r2:=reactant2</label><nail x=\"-960\" y=\"-720\"/><nail x=\"-1328\" y=\"-720\"/></transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1584\" y=\"-880\">timeL[reactant2][reactant1]\n   != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1504\" y=\"-848\">r1 := reactant1,\nr2 := reactant2,\nc := 0</label></transition></template>").getBytes()));
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

}

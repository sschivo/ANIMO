package inat.analyser.uppaal;

import inat.model.Model;
import inat.model.Property;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.util.Table;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
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
 * This class converts the given model into a variable based UPPAAL model.
 * This model is the one with priorities. Notice that UPAAL cannot generate
 * concrete simulation traces from this model.
 * 
 * @author Brend Wanders
 * 
 */
public class VariablesModel implements ModelTransformer {

	protected static final String CATALYST = Model.Properties.CATALYST,
								NUMBER_OF_LEVELS = Model.Properties.NUMBER_OF_LEVELS,
								TIMES_UPPER = Model.Properties.TIMES_UPPER,
								TIMES = Model.Properties.TIMES,
								TIMES_LOWER = Model.Properties.TIMES_LOWER,
								INCREMENT = Model.Properties.INCREMENT,
								BI_REACTION = Model.Properties.BI_REACTION,
								MONO_REACTION = Model.Properties.MONO_REACTION,
								REACTANT = Model.Properties.REACTANT,
								REACTION_TYPE = Model.Properties.REACTION_TYPE,
								ALIAS = Model.Properties.ALIAS,
								ENABLED = Model.Properties.ENABLED,
								GROUP = Model.Properties.GROUP,
								INITIAL_LEVEL = Model.Properties.INITIAL_LEVEL;
	public static final int INFINITE_TIME = -1;
	protected static String newLine = System.getProperty("line.separator");
	Map<String, Vector<Reactant>> groups = null;

	@Override
	public String transform(Model m) {
		StringBuilder out = new StringBuilder();

		this.appendModel(out, m);

		String result = out.toString();
		out.setLength(0); //Attempt at saving memory (not enough)
		out.trimToSize();
		out = null;
		
		return result;
	}

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
		//out.append("const int MAX_LEVELS = " + m.getProperties().get("levels").as(Integer.class) + ";");
		//out.append(newLine);
		int countReactions = 0;
		for (Reaction r : m.getReactionCollection()) {
			if (r.get(ENABLED).as(Boolean.class)) {
				countReactions++;
			}
		}
		out.append("const int N_REACTIONS = " + countReactions + ";");
		out.append(newLine);
		out.append("broadcast chan update;");
		out.append(newLine);
		out.append("chan reaction_happening[N_REACTIONS];");
		out.append(newLine);
		out.append("chan update_done[N_REACTIONS];");
		out.append(newLine);
		//out.append("chan priority update &lt; reaction_happening;");
		out.append("chan priority update");
		for (int i = 0; i < countReactions; i++) {
			out.append(" &lt; reaction_happening[" + i + "]");
		}
		for (int i = 0; i < countReactions; i++) {
			out.append(" &lt; update_done[" + i + "]");
		}
		out.append(";");
		out.append(newLine);
		out.append(newLine);
		for (Reactant r : m.getReactantCollection()) {
			if (r.get(ENABLED).as(Boolean.class)) {
				this.appendReactantVariables(out, r);
			}
		}
		out.append("</declaration>");

		out.append(newLine);
		out.append(newLine);
		// output templates
		this.appendTemplates(out, m);

		out.append(newLine);
		out.append("<system>");
		out.append(newLine);
		
		out.append("Coord = Coordinator(reaction_happening, update, update_done);");
		out.append(newLine);
		out.append(newLine);

		// output the process instantiation for each reactant and reaction
		for (Reactant r : m.getReactantCollection()) {
			if (r.get(ENABLED).as(Boolean.class)) {
				if (r.get(GROUP) == null || r.get(GROUP).isNull() || r.get(GROUP).as(String.class).length() < 1) {
					this.appendReactantProcesses(out, r);
				}
			}
		}
		for (String group : groups.keySet()) {
			this.appendReactantGroupProcess(out, group);
		}
		out.append(newLine);
		int reactionIndex = 0;
		for (Reaction r : m.getReactionCollection()) {
			if (r.get(ENABLED).as(Boolean.class)) {
				this.appendReactionProcesses(out, m, r, reactionIndex);
				reactionIndex++;
			}
		}
		out.append(newLine);
		out.append(newLine);
		
		/*out.append("Crono = crono();");
		out.append(newLine);
		out.append(newLine);*/
		
		// compose the system
		out.append("system ");
		for (Reactant r : m.getReactantCollection()) {
			if (r.get(ENABLED).as(Boolean.class)) {
				if (r.get(GROUP) == null || r.get(GROUP).isNull() || r.get(GROUP).as(String.class).length() < 1) {
					out.append(r.getId() + "_reactant, ");
				}
			}
		}
		for (String group : groups.keySet()) {
			out.append(group + "_group, ");
		}
		for (Reaction r : m.getReactionCollection()) {
			if (r.get(ENABLED).as(Boolean.class)) {
				out.append(getReactionName(r) + ", ");
			}
		}
		//out.append("Coord, Crono;");
		out.append("Coord;");

		out.append(newLine);
		out.append(newLine);
		out.append("</system>");
		out.append(newLine);
		out.append("</nta>");
	}

	protected String formatTime(int time) {
		if (time == INFINITE_TIME) {
			return "INFINITE_TIME";
		} else {
			return "" + time;
		}
	}

	/**
	 * Determines the name of a reaction process.
	 * 
	 * @param r the reaction to name
	 * @return the name of the process
	 */
	protected String getReactionName(Reaction r) {
		if (r.get(REACTION_TYPE).as(String.class).equals(MONO_REACTION)) {
			// reaction1 is assumed to be a degredation reaction
			String reactantId = r.get(REACTANT).as(String.class);
			return reactantId + "_deg";
		} else if (r.get(REACTION_TYPE).as(String.class).equals(BI_REACTION)) {
			String r1Id = r.get(CATALYST).as(String.class);
			String r2Id = r.get(REACTANT).as(String.class);
			return r1Id + "_" + r2Id + "_r_" + ((r.get(INCREMENT).as(Integer.class) >= 0) ? "up" : "down");
		} else {
			return null;
		}
	}

	protected void appendReactionProcesses(StringBuilder out, Model m, Reaction r, int index) {
		if (r.get(REACTION_TYPE).as(String.class).equals(MONO_REACTION)) {
			String reactantId = r.get(REACTANT).as(String.class);
			out.append("//Mono-reaction on " + reactantId + " (" + m.getReactant(reactantId).get(ALIAS).as(String.class) + ")");
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
			assert timesL.getColumnCount() == 1 : "Table LowerBound is (larger than one)-dimensional.";
			assert timesU.getColumnCount() == 1 : "Table UpperBound is (larger than one)-dimensional.";
			assert timesL.getRowCount() == m.getReactant(reactantId).get(NUMBER_OF_LEVELS).as(Integer.class) + 1 : "Incorrect number of rows in 'timesLower' table of '" + r + "'";
			assert timesU.getRowCount() == m.getReactant(reactantId).get(NUMBER_OF_LEVELS).as(Integer.class) + 1 : "Incorrect number of rows in 'timesUpper' table of '" + r + "'";
			
			// output times table constants for this reaction (lower bound)
			out.append("const int " + reactantId + "_tLower[" + m.getReactant(reactantId).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
			for (int i = 0; i < timesL.getRowCount() - 1; i++) {
				out.append(formatTime(timesL.get(i, 0)) + ", ");
			}
			out.append(formatTime(timesL.get(timesL.getRowCount() - 1, 0)) + "};");
			out.append(newLine);
			
			// output times table constants for this reaction (upper bound)
			out.append("const int " + reactantId + "_tUpper[" + m.getReactant(reactantId).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
			for (int i = 0; i < timesU.getRowCount() - 1; i++) {
				out.append(formatTime(timesU.get(i, 0)) + ", ");
			}
			out.append(formatTime(timesU.get(timesU.getRowCount() - 1, 0)) + "};");
			out.append(newLine);

			// output reaction instantiation
			final String name = getReactionName(r);
			out.append(name + " = Reaction_" + reactantId + "(" + reactantId + ", " + reactantId + "_nonofficial, " + reactantId + "_tLower, "
					+ reactantId + "_tUpper, " + r.get(INCREMENT).as(Integer.class) + ", update, reaction_happening[" + index + "], update_done[" + index + "]);");
			out.append(newLine);
			out.append(newLine);

		} else if (r.get(REACTION_TYPE).as(String.class).equals(BI_REACTION)) {
			String r1Id = r.get(CATALYST).as(String.class);
			String r2Id = r.get(REACTANT).as(String.class);
			out.append("//Reaction " + r1Id + " (" + m.getReactant(r1Id).get(ALIAS).as(String.class) + ") " + (r.get(INCREMENT).as(Integer.class)>0?"-->":"--|") + " " + r2Id + " (" + m.getReactant(r2Id).get(ALIAS).as(String.class) + ")");
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
			out.append("const int " + r1Id + "_" + r2Id + "_r_tLower[" + m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
			out.append(newLine);
			
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

			out.append("};");
			out.append(newLine);
			
			// output times table constant for this reaction
			out.append("const int " + r1Id + "_" + r2Id + "_r_tUpper[" + m.getReactant(r2Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r1Id).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1] := {");
			out.append(newLine);

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

			out.append("};");
			out.append(newLine);
			out.append(newLine);

			// output process instantiation
			final String name = getReactionName(r);
			out.append(name + " = Reaction2_" + r1Id + "_" + r2Id + "(" + r1Id + ", " + r1Id + "_nonofficial, " + r2Id + ", " + r2Id + "_nonofficial, " + r1Id + "_" + r2Id
					+ "_r_tLower, " + r1Id + "_" + r2Id + "_r_tUpper, " + r.get(INCREMENT).as(Integer.class)
					+ ", update, reaction_happening[" + index + "], update_done[" + index + "]);");
			out.append(newLine);
			out.append(newLine);
		}
	}

	protected void appendReactantProcesses(StringBuilder out, Reactant r) {
		// output process instantiation
		out.append(r.getId() + "_reactant = Reactant_" + r.getId() + "(" + r.getId() + ", " + r.getId() + "_nonofficial, update);");
		out.append(newLine);
		out.append(newLine);
	}
	
	protected void appendReactantGroupProcess(StringBuilder out, String group) {
		out.append(group + "_group = Reactant_group_" + group + "(");
		for (Reactant r : groups.get(group)) {
			out.append(r.getId() + ", " + r.getId() + "_nonofficial, ");
		}
		out.append("update);");
		out.append(newLine);
		out.append(newLine);
	}

	protected void appendTemplates(StringBuilder out, Model m) {
		try {
			StringWriter outString;
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document;
			Transformer tra = TransformerFactory.newInstance().newTransformer();
			tra.setOutputProperty(OutputKeys.INDENT, "yes");
			tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			//This should have been a "Chronometer" process to force the update of globalTime for each simulation step, but it also curiously forces the UPPAAL engine to always choose the shortest simulation traces possible, thus voiding all the time intervals for reactions
			/*outString = new StringWriter();
			document = documentBuilder.parse(new ByteArrayInputStream(("<template><name>crono</name><declaration>int[0, 1073741821] metro := 0;</declaration><location id=\"id0\" x=\"0\" y=\"0\"><label kind=\"invariant\" x=\"-176\" y=\"-24\">globalTime&lt;=metro+1</label></location><init ref=\"id0\"/><transition><source ref=\"id0\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"56\" y=\"-24\">globalTime&gt;=metro</label><label kind=\"assignment\" x=\"56\" y=\"0\">metro:=metro+1</label><nail x=\"56\" y=\"-48\"/><nail x=\"56\" y=\"48\"/></transition></template>").getBytes()));
			tra.transform(new DOMSource(document), new StreamResult(outString));
			out.append(outString.toString());
			out.append(newLine);
			out.append(newLine);*/
			for (Reaction r : m.getReactionCollection()) {
				if (!r.get(ENABLED).as(Boolean.class)) continue;
				outString = new StringWriter();
				if (r.get(REACTION_TYPE).as(String.class).equals(BI_REACTION)) {
					document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction2_" + r.get(CATALYST).as(String.class) + "_" + r.get(REACTANT).as(String.class) + "</name><parameter>int[0," + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "] &amp;reactant1, int &amp;reactant1_nonofficial, int[0," + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "] &amp;reactant2, int &amp;reactant2_nonofficial, const int timeL[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1][" + m.getReactant(r.get(CATALYST).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int delta, broadcast chan &amp;update, chan &amp;inform_reacting, chan &amp;inform_updated</parameter><declaration>clock c;</declaration><location id=\"id0\" x=\"-1816\" y=\"-736\"></location><location id=\"id1\" x=\"-1816\" y=\"-1128\"></location><location id=\"id2\" x=\"-1552\" y=\"-976\"><committed/></location><location id=\"id3\" x=\"-1816\" y=\"-872\"><label kind=\"invariant\" x=\"-2152\" y=\"-896\">timeU[reactant2][reactant1] == INFINITE_TIME\n|| c&lt;=timeU[reactant2][reactant1]</label></location><location id=\"id4\" x=\"-1816\" y=\"-1016\"><committed/></location><init ref=\"id4\"/><transition><source ref=\"id3\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-2096\" y=\"-832\">reactant1 == reactant1_nonofficial\n&amp;&amp; reactant2 == reactant2_nonofficial</label><label kind=\"synchronisation\" x=\"-1976\" y=\"-800\">update?</label><nail x=\"-1856\" y=\"-832\"/><nail x=\"-1920\" y=\"-832\"/><nail x=\"-1920\" y=\"-776\"/><nail x=\"-1856\" y=\"-776\"/></transition><transition><source ref=\"id1\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1928\" y=\"-1248\">reactant1 == reactant1_nonofficial\n&amp;&amp; reactant2 == reactant2_nonofficial</label><label kind=\"synchronisation\" x=\"-1840\" y=\"-1224\">update?</label><nail x=\"-1776\" y=\"-1208\"/><nail x=\"-1864\" y=\"-1208\"/></transition><transition><source ref=\"id1\"/><target ref=\"id4\"/><label kind=\"guard\" x=\"-2088\" y=\"-1160\">reactant1 != reactant1_nonofficial\n|| reactant2 != reactant2_nonofficial</label><label kind=\"synchronisation\" x=\"-2032\" y=\"-1128\">update?</label><nail x=\"-1968\" y=\"-1128\"/><nail x=\"-1968\" y=\"-1016\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1736\" y=\"-960\">reactant1 != reactant1_nonofficial\n|| reactant2 != reactant2_nonofficial</label><label kind=\"synchronisation\" x=\"-1728\" y=\"-936\">update?</label><nail x=\"-1728\" y=\"-920\"/><nail x=\"-1624\" y=\"-920\"/></transition><transition><source ref=\"id0\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1776\" y=\"-752\">update?</label><nail x=\"-1424\" y=\"-736\"/><nail x=\"-1424\" y=\"-896\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1792\" y=\"-1144\">timeL[reactant2][reactant1] == INFINITE_TIME</label><label kind=\"synchronisation\" x=\"-1792\" y=\"-1160\">inform_updated?</label><nail x=\"-1464\" y=\"-976\"/><nail x=\"-1464\" y=\"-1128\"/></transition><transition><source ref=\"id4\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1952\" y=\"-1080\">timeL[reactant2][reactant1]== INFINITE_TIME</label><label kind=\"synchronisation\" x=\"-1872\" y=\"-1096\">inform_updated?</label></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1784\" y=\"-1032\">timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1784\" y=\"-1008\">inform_updated?</label><label kind=\"assignment\" x=\"-1784\" y=\"-992\">c:=timeU[reactant2][reactant1]</label><nail x=\"-1736\" y=\"-976\"/></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1760\" y=\"-912\">(timeU[reactant2][reactant1] == INFINITE_TIME &amp;&amp; timeL[reactant2][reactant1] != INFINITE_TIME)\n|| (timeU[reactant2][reactant1] != INFINITE_TIME &amp;&amp; c&lt;=timeU[reactant2][reactant1])</label><label kind=\"synchronisation\" x=\"-1760\" y=\"-888\">inform_updated?</label><nail x=\"-1552\" y=\"-872\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"-1808\" y=\"-840\">c&gt;=timeL[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1808\" y=\"-824\">inform_reacting!</label><label kind=\"assignment\" x=\"-1808\" y=\"-808\">reactant2_nonofficial := reactant2_nonofficial + delta,\nc:=0</label></transition><transition><source ref=\"id4\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1968\" y=\"-976\">timeL[reactant2][reactant1]\n!= INFINITE_TIME</label><label kind=\"synchronisation\" x=\"-1968\" y=\"-952\">inform_updated?</label><label kind=\"assignment\" x=\"-1968\" y=\"-936\">c:=0</label></transition></template>").getBytes()));
				} else {
					document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get(REACTANT).as(String.class) + "</name><parameter>int[0," + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "] &amp;reactant, int &amp;reactant_nonofficial, const int timeL[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get(REACTANT).as(String.class)).get(NUMBER_OF_LEVELS).as(Integer.class) + "+1], const int delta, broadcast chan &amp;update, chan &amp;inform_reacting, chan &amp;inform_updated</parameter><declaration>clock c;</declaration><location id=\"id5\" x=\"-1320\" y=\"-480\"></location><location id=\"id6\" x=\"-1320\" y=\"-920\"></location><location id=\"id7\" x=\"-1128\" y=\"-712\"><committed/></location><location id=\"id8\" x=\"-1320\" y=\"-624\"><label kind=\"invariant\" x=\"-1568\" y=\"-648\">timeU[reactant] == INFINITE_TIME\n|| c&lt;=timeU[reactant]</label></location><location id=\"id9\" x=\"-1320\" y=\"-816\"><committed/></location><init ref=\"id9\"/><transition><source ref=\"id8\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1552\" y=\"-560\">reactant == reactant_nonofficial</label><label kind=\"synchronisation\" x=\"-1496\" y=\"-544\">update?</label><nail x=\"-1376\" y=\"-560\"/><nail x=\"-1440\" y=\"-560\"/><nail x=\"-1440\" y=\"-504\"/><nail x=\"-1360\" y=\"-504\"/></transition><transition><source ref=\"id6\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1424\" y=\"-1016\">reactant == reactant_nonofficial</label><label kind=\"synchronisation\" x=\"-1352\" y=\"-1000\">update?</label><nail x=\"-1288\" y=\"-984\"/><nail x=\"-1360\" y=\"-984\"/></transition><transition><source ref=\"id6\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1600\" y=\"-936\">reactant != reactant_nonofficial</label><label kind=\"synchronisation\" x=\"-1544\" y=\"-920\">update?</label><nail x=\"-1480\" y=\"-920\"/><nail x=\"-1480\" y=\"-816\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1264\" y=\"-696\">reactant != reactant_nonofficial</label><label kind=\"synchronisation\" x=\"-1264\" y=\"-688\">update?</label><nail x=\"-1264\" y=\"-672\"/><nail x=\"-1168\" y=\"-672\"/></transition><transition><source ref=\"id5\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1256\" y=\"-496\">update?</label><nail x=\"-944\" y=\"-480\"/><nail x=\"-944\" y=\"-656\"/></transition><transition><source ref=\"id7\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1288\" y=\"-936\">timeL[reactant] == INFINITE_TIME</label><label kind=\"synchronisation\" x=\"-1288\" y=\"-952\">inform_updated?</label><nail x=\"-1040\" y=\"-712\"/><nail x=\"-1040\" y=\"-920\"/></transition><transition><source ref=\"id9\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1440\" y=\"-864\">timeL[reactant] == INFINITE_TIME</label><label kind=\"synchronisation\" x=\"-1376\" y=\"-880\">inform_updated?</label></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1272\" y=\"-768\">timeU[reactant] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant]</label><label kind=\"synchronisation\" x=\"-1272\" y=\"-744\">inform_updated?</label><label kind=\"assignment\" x=\"-1272\" y=\"-728\">c:=timeU[reactant]</label><nail x=\"-1264\" y=\"-712\"/></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1280\" y=\"-664\">(timeU[reactant] == INFINITE_TIME &amp;&amp; timeL[reactant] != INFINITE_TIME)\n|| (timeU[reactant] != INFINITE_TIME &amp;&amp; c&lt;=timeU[reactant])</label><label kind=\"synchronisation\" x=\"-1280\" y=\"-640\">inform_updated?</label><nail x=\"-1128\" y=\"-624\"/></transition><transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1312\" y=\"-584\">c&gt;=timeL[reactant]</label><label kind=\"synchronisation\" x=\"-1312\" y=\"-568\">inform_reacting!</label><label kind=\"assignment\" x=\"-1312\" y=\"-552\">reactant_nonofficial := reactant_nonofficial + delta,\nc:=0</label></transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1424\" y=\"-792\">timeL[reactant] != INFINITE_TIME</label><label kind=\"synchronisation\" x=\"-1424\" y=\"-776\">inform_updated?</label><label kind=\"assignment\" x=\"-1424\" y=\"-760\">c:=0</label><nail x=\"-1320\" y=\"-656\"/></transition></template>").getBytes()));
				}
				tra.transform(new DOMSource(document), new StreamResult(outString));
				out.append(outString.toString());
				out.append(newLine);
				out.append(newLine);
			}
			
			//This could possibly be removed.
			//It is used to represent with a special template the "groups of reactants", which represent all the same molecule, with alternative, exclusive phosphorylation possibilities.
			//The GROUP property for each node of the group needs to be set to the same value to represent this situation.
			groups = new HashMap<String, Vector<Reactant>>();
			for (Reactant r : m.getReactantCollection()) {
				if (!r.get(ENABLED).as(Boolean.class)) continue;
				if (r.get(GROUP) != null && !r.get(GROUP).isNull() && r.get(GROUP).as(String.class).length() > 0) { //you simply have to set equal values for the GROUP property, and the Reactant will be output just for that group, as we see below
					String group = r.get(GROUP).as(String.class);
					if (groups.containsKey(group)) {
						groups.get(group).add(r);
					} else {
						Vector<Reactant> v = new Vector<Reactant>();
						v.add(r);
						groups.put(group, v);
					}
					continue;
				}
				outString = new StringWriter();
				document = documentBuilder.parse(new ByteArrayInputStream(("<template><name>Reactant_" + r.getId() + "</name><parameter>int[0," + r.get(NUMBER_OF_LEVELS).as(Integer.class) + "] &amp;official, int &amp;nonofficial, broadcast chan &amp;update</parameter><location id=\"id10\" x=\"-416\" y=\"-104\"></location><init ref=\"id10\"/><transition><source ref=\"id10\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-536\" y=\"-248\">nonofficial&gt;" + r.get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-536\" y=\"-232\">update?</label><label kind=\"assignment\" x=\"-536\" y=\"-216\">official := " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + ", nonofficial := " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><nail x=\"-168\" y=\"-200\"/><nail x=\"-168\" y=\"-256\"/><nail x=\"-544\" y=\"-256\"/><nail x=\"-544\" y=\"-192\"/><nail x=\"-416\" y=\"-192\"/></transition><transition><source ref=\"id10\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-496\" y=\"-48\">nonofficial&lt;0</label><label kind=\"synchronisation\" x=\"-496\" y=\"-32\">update?</label><label kind=\"assignment\" x=\"-496\" y=\"-16\">official := 0, nonofficial := 0</label><nail x=\"-416\" y=\"-56\"/><nail x=\"-504\" y=\"-56\"/><nail x=\"-504\" y=\"8\"/><nail x=\"-288\" y=\"8\"/><nail x=\"-288\" y=\"-24\"/></transition><transition><source ref=\"id10\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-680\" y=\"-176\">nonofficial&gt;=0\n&amp;&amp; nonofficial&lt;=" + r.get(NUMBER_OF_LEVELS).as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-680\" y=\"-144\">update?</label><label kind=\"assignment\" x=\"-680\" y=\"-128\">official := nonofficial</label><nail x=\"-688\" y=\"-104\"/><nail x=\"-688\" y=\"-184\"/><nail x=\"-464\" y=\"-184\"/></transition></template>").getBytes()));
				tra.transform(new DOMSource(document), new StreamResult(outString));
				out.append(outString.toString());
				out.append(newLine);
				out.append(newLine);
			}
			
			if (!groups.isEmpty()) { //compose the Reactant for this group
				for (String group : groups.keySet()) {
					Vector<Reactant> v = groups.get(group);
					outString = new StringWriter();
					StringBuilder templateString = new StringBuilder();
					templateString.append("<template><name>Reactant_group_" + group + "</name><parameter>");
					for (int i=0; i<v.size();i++) {
						Reactant r = v.elementAt(i);
						templateString.append("int[0," + r.get(NUMBER_OF_LEVELS).as(Integer.class) + "] &amp;official" + (i + 1) + ", int &amp;unofficial" + (i + 1) + ", ");
					}
					templateString.append("broadcast chan &amp;update</parameter><declaration>void updateAll(");
					for (int i=0; i<v.size() - 1;i++) {
						Reactant r = v.elementAt(i);
						templateString.append("int[0," + r.get(NUMBER_OF_LEVELS).as(Integer.class) + "] &amp;official" + (i + 1) + ", int &amp;unofficial" + (i + 1) + ", ");
					}
					templateString.append("int[0," + v.lastElement().get(NUMBER_OF_LEVELS).as(Integer.class) + "] &amp;official" + v.size() + ", int &amp;unofficial" + v.size() + ") {\n\tint i;\n\tint sum := 0;\n");
					for (int i=0; i<v.size();i++) {
						templateString.append("\tif (unofficial" + (i + 1) + " &lt; 0) unofficial" + (i + 1) + " := 0;\n\tsum := sum + unofficial" + (i + 1) + ";\n");
					}
					//TODO: v.firstElement().get("levels").as(Integer.class) is the number of levels of the "grouped" reactant. So, we implicitly assume that all reactants in a group have the same NUMBER_OF_LEVELS
					templateString.append("\n\twhile (sum &gt; " + v.firstElement().get(NUMBER_OF_LEVELS).as(Integer.class) + ") {\n\t\tsum := 0;\n");
					for (int i=0; i<v.size(); i++) {
						templateString.append("\t\tif (unofficial" + (i + 1) + " &gt; 0) unofficial" + (i + 1) + "--;\n\t\tsum := sum + unofficial" + (i + 1) + ";\n");
					}
					templateString.append("\t}\n");
					for (int i=0; i<v.size(); i++) {
						templateString.append("\tofficial" + (i + 1) + " := unofficial" + (i + 1) + ";\n");
					}
					templateString.append("}</declaration><location id=\"id5\" x=\"16\" y=\"88\"></location><init ref=\"id5\"/><transition><source ref=\"id5\"/><target ref=\"id5\"/><label kind=\"synchronisation\" x=\"-24\" y=\"-88\">update?</label><label kind=\"assignment\" x=\"-144\" y=\"-64\">updateAll(");
					for (int i=0; i<v.size() - 1;i++) {
						templateString.append("official" + (i + 1) + ", unofficial" + (i + 1) + ", ");
					}
					templateString.append("official" + v.size() + ", unofficial" + v.size() + ")</label><nail x=\"72\" y=\"-40\"/><nail x=\"-48\" y=\"-40\"/></transition></template>");
					document = documentBuilder.parse(new ByteArrayInputStream(templateString.toString().getBytes()));
					tra.transform(new DOMSource(document), new StreamResult(outString));
					out.append(outString.toString());
					out.append(newLine);
					out.append(newLine);
				}
			}
			
			outString = new StringWriter();
			document = documentBuilder.parse(new ByteArrayInputStream("<template><name>Coordinator</name><parameter>chan &amp;reaction_happening[N_REACTIONS], broadcast chan &amp;update, chan &amp;update_done[N_REACTIONS]</parameter><location id=\"id11\" x=\"-328\" y=\"-136\"><name x=\"-338\" y=\"-166\">updated</name></location><location id=\"id12\" x=\"-152\" y=\"-136\"><committed/></location><init ref=\"id11\"/><transition><source ref=\"id11\"/><target ref=\"id11\"/><label kind=\"select\" x=\"-552\" y=\"-152\">i : int[0,N_REACTIONS-1]</label><label kind=\"synchronisation\" x=\"-552\" y=\"-136\">update_done[i]!</label><nail x=\"-392\" y=\"-176\"/><nail x=\"-392\" y=\"-96\"/></transition><transition><source ref=\"id12\"/><target ref=\"id11\"/><label kind=\"synchronisation\" x=\"-312\" y=\"-80\">update!</label><nail x=\"-152\" y=\"-64\"/><nail x=\"-328\" y=\"-64\"/></transition><transition><source ref=\"id12\"/><target ref=\"id12\"/><label kind=\"select\" x=\"-80\" y=\"-160\">i : int[0,N_REACTIONS-1]</label><label kind=\"synchronisation\" x=\"-80\" y=\"-144\">reaction_happening[i]?</label><nail x=\"-88\" y=\"-176\"/><nail x=\"-88\" y=\"-104\"/></transition><transition><source ref=\"id11\"/><target ref=\"id12\"/><label kind=\"select\" x=\"-320\" y=\"-248\">i : int[0, N_REACTIONS-1]</label><label kind=\"synchronisation\" x=\"-320\" y=\"-232\">reaction_happening[i]?</label><nail x=\"-328\" y=\"-216\"/><nail x=\"-152\" y=\"-216\"/></transition></template>".getBytes()));
			tra.transform(new DOMSource(document), new StreamResult(outString));
			out.append(outString.toString());
			out.append(newLine);
			out.append(newLine);
		} catch (Exception e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
		}
	}

	protected void appendReactantVariables(StringBuilder out, Reactant r) {
		// outputs the global variables necessary for the given reactant
		out.append("//" + r.getId() + " = " + r.get(ALIAS).as(String.class));
		out.append(newLine);
		out.append("int[0," + r.get(NUMBER_OF_LEVELS).as(Integer.class) + "] " + r.getId() + " := " + r.get(INITIAL_LEVEL).as(Integer.class) + ";");
		out.append(newLine);
		out.append("int " + r.getId() + "_nonofficial := " + r.get(INITIAL_LEVEL).as(Integer.class) + ";");
		out.append(newLine);
		out.append(newLine);
	}

}

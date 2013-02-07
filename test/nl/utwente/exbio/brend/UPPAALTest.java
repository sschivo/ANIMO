package nl.utwente.exbio.brend;

import inat.InatBackend;
import inat.analyser.LevelResult;
import inat.analyser.uppaal.UppaalModelAnalyserFasterConcrete;
import inat.exceptions.InatException;
import inat.graph.Graph;
import inat.model.Model;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.serializer.CsvWriter;
import inat.serializer.XMLSerializer;
import inat.util.Table;
import inat.util.XmlEnvironment;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * The UPPAAL test class.
 * 
 * @author Brend Wanders
 * 
 */
public class UPPAALTest {
	/**
	 * Program entry point.
	 * 
	 * @param args the command line arguments
	 * @throws Exception 
	 * @throws SAXException if the XML didn't parse
	 * @throws ParserConfigurationException 
	 */
	public static void main(String[] args) throws InatException, IOException, SAXException, ParserConfigurationException {
		InatBackend.initialise(new File("ANIMO-configuration.xml"));
		System.setProperty("java.security.policy", new File("ANIMO-security.policy").getAbsolutePath());

		// create model
		Model model = new Model();
		model.getProperties().let("levels").be(4);

		Reactant a = new Reactant("a");
		a.let("name").be("A");
		a.let("initialConcentration").be(3);
		model.add(a);

		Reactant b = new Reactant("b");
		b.let("name").be("B");
		b.let("initialConcentration").be(1);
		model.add(b);

		Reaction deg = new Reaction("aDeg");
		deg.let("type").be("reaction1");
		deg.let("reactant").be("a");
		deg.let("increment").be(-1);
		Table deactivationTable = new Table(4 + 1, 1);
		deactivationTable.set(0, 0, -1);
		deactivationTable.set(1, 0, 150);
		deactivationTable.set(2, 0, 130);
		deactivationTable.set(3, 0, 120);
		deactivationTable.set(4, 0, 110);
		deg.let("times").be(deactivationTable);
		model.add(deg);

		Reaction degb = new Reaction("bDeg");
		degb.let("type").be("reaction1");
		degb.let("reactant").be("b");
		degb.let("increment").be(-1);
		degb.let("times").be(deactivationTable);
		model.add(degb);

		Reaction r = new Reaction("foo");
		r.let("type").be("reaction2");
		r.let("reactant").be("b");
		r.let("catalyst").be("a");
		r.let("increment").be(+1);
		Table reactionTable = new Table(4 + 1, 4 + 1);
		for (int i = 0; i < reactionTable.getColumnCount(); i++) {
			reactionTable.set(0, i, -1);
		}

		for (int i = 0; i < reactionTable.getColumnCount(); i++) {
			for (int j = 1; j < reactionTable.getRowCount(); j++) {
				reactionTable.set(j, i, 70 - j * 10);
			}
		}
		r.let("times").be(reactionTable);
		model.add(r);

		model = new XMLSerializer().deserializeModel(XmlEnvironment.parse(new File("Z:/test.xml")));
		// composite the analyser (this should be done from configuration)
		//ModelAnalyser<LevelResult> analyzer = new UppaalModelAnalyser(new VariablesInterpreter(), new VariablesModel());

		// analyse model
		File file = new File("my_example.xml");
		  DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		  DocumentBuilder db = dbf.newDocumentBuilder();
		  Document doc = db.parse(file);
		model = new XMLSerializer().deserializeModel(doc);
		LevelResult result = new UppaalModelAnalyserFasterConcrete(null, null).analyze(model, 1200);
		//result = new UPPAALClient("ewi1735.ewi.utwente.nl", 1234).analyze(model, 500);

		// output result
		System.out.println(result);

		CsvWriter writer = new CsvWriter();
		writer.writeCsv("ExampleCSV.csv", model, result);
		Graph.plotGraph(new File("ExampleCSV.csv"));
		Graph.exitOnClose();
	}
}

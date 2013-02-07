/**
 * 
 */
package inat.serializer;

import inat.exceptions.SerializationException;
import inat.util.AXPathExpression;
import inat.util.Table;
import inat.util.XmlEnvironment;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The {@link Table} serializer.
 * 
 * @author B. Wanders
 */
public class TableSerializer implements TypeSerializer<Table> {
	/**
	 * Patterns.
	 */
	private final AXPathExpression xpRows = XmlEnvironment.hardcodedXPath("./table/@rows");
	private final AXPathExpression xpColumns = XmlEnvironment.hardcodedXPath("./table/@columns");

	private final AXPathExpression xpCells = XmlEnvironment.hardcodedXPath("./table/cell");
	private final AXPathExpression xpRow = XmlEnvironment.hardcodedXPath("@row");
	private final AXPathExpression xpColumn = XmlEnvironment.hardcodedXPath("@column");
	private final AXPathExpression xpText = XmlEnvironment.hardcodedXPath(".");

	@Override
	public Table deserialize(Node root) throws SerializationException {
		Table result = null;
		try {
			int rows = Integer.parseInt(this.xpRows.getString(root));
			int cols = Integer.parseInt(this.xpColumns.getString(root));
			result = new Table(rows, cols);

			for (Node cell : this.xpCells.getNodes(root)) {
				int row = Integer.parseInt(this.xpRow.getString(cell));
				int col = Integer.parseInt(this.xpColumn.getString(cell));
				int val = Integer.parseInt(this.xpText.getString(cell));
				result.set(row, col, val);
			}

		} catch (NumberFormatException e) {
			throw new SerializationException("Could not interpret an integer while deserializing a table.", e);
		} catch (XPathExpressionException e) {
			throw new SerializationException("Could not evaluate XPath expression while deserializing a table.", e);
		}

		return result;
	}

	@Override
	public Node serialize(Document doc, Object value) {
		Table t = (Table) value;
		Element table = doc.createElement("table");
		table.setAttribute("rows", "" + t.getRowCount());
		table.setAttribute("columns", "" + t.getColumnCount());

		for (int r = 0; r < t.getRowCount(); r++) {
			for (int c = 0; c < t.getColumnCount(); c++) {
				if (t.get(r, c) != 0) {
					Element cell = doc.createElement("cell");
					cell.setAttribute("row", "" + r);
					cell.setAttribute("column", "" + c);
					cell.setTextContent("" + t.get(r, c));
					table.appendChild(cell);
				}
			}
		}

		return table;
	}
}

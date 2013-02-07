package inat.graph;

/*
 * This code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this program; if not, write to the Free 
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, 
 * MA  02111-1307, USA.
 */



import java.util.ArrayList;



/**
 * A StringTokenizer class that handle empty tokens.
 * 
 * @author <a href="mailto:info@geosoft.no">GeoSoft</a>
 */   
public class SmartTokenizer {
	private ArrayList<String> tokens_;
	private int       current_;



	public SmartTokenizer (String string, String delimiter) {
		tokens_  = new ArrayList<String>();
		current_ = 0;

		java.util.StringTokenizer tokenizer =
			new java.util.StringTokenizer (string, delimiter, true);

		boolean wasDelimiter = true;
		boolean isDelimiter  = false;    

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();

			isDelimiter = token.equals (delimiter);

			if (wasDelimiter)
				tokens_.add (isDelimiter ? "" : token);
			else if (!isDelimiter)
				tokens_.add (token);

			wasDelimiter = isDelimiter;
		}

		if (isDelimiter) tokens_.add ("");
	}


	public int countTokens() {
		return tokens_.size();
	}


	public boolean hasMoreTokens() {
		return current_ < tokens_.size();
	}


	public boolean hasMoreElements() {
		return hasMoreTokens();
	}


	public Object nextElement() {
		return nextToken();
	}


	public String nextToken() {
		if (current_ >= tokens_.size()) return null; //This way it is even "smarter"
		String token = tokens_.get (current_);
		current_++;
		return token;
	}



	/**
	 * Testing this class.
	 * 
	 * @param args  Not used.
	 */
	public static void main (String args[]) {
		SmartTokenizer t = new SmartTokenizer ("This,is,a,,test,", ",");
		while (t.hasMoreTokens()) {
			String token = t.nextToken();
			System.out.println ("#" + token + "#");
		}
	}
}



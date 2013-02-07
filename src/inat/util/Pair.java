package inat.util;

public class Pair<T1, T2> {
	public T1 first;
	public T2 second;
	
	public Pair() {
	}
	
	public Pair(T1 first, T2 second) {
		this.first = first;
		this.second = second;
	}
	
	/**
	 * returns a (deep) copy of this
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Pair<T1, T2> copy() {
		Pair<T1, T2> pair = new Pair<T1, T2>();
		Class c1 = this.first.getClass(),
			  c2 = this.second.getClass();
		if (Table.class.equals(c1)) {
			pair.first = (T1)((Table)this.first).copy();
		} else if (Pair.class.equals(c1)) {
			pair.first = (T1)((Pair)this.first).copy();
		} else if (Triple.class.equals(c1)) {
			pair.first = (T1)((Triple)this.first).copy();
		} else if (Quadruple.class.equals(c1)) {
			pair.first = (T1)((Quadruple)this.first).copy();
		} else {
			pair.first = this.first;
		}
		if (Table.class.equals(c2)) {
			pair.second = (T2)((Table)this.second).copy();
		} else if (Pair.class.equals(c2)) {
			pair.second = (T2)((Pair)this.second).copy();
		} else if (Triple.class.equals(c2)) {
			pair.second = (T2)((Triple)this.second).copy();
		} else if (Quadruple.class.equals(c2)) {
			pair.second = (T2)((Quadruple)this.second).copy();
		} else {
			pair.second = this.second;
		}
		return pair;
	}
}

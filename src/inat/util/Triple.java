package inat.util;

public class Triple<T1, T2, T3> {
	public T1 first;
	public T2 second;
	public T3 third;
	
	public Triple() {
	}
	
	public Triple(T1 first, T2 second, T3 third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	/**
	 * returns a (deep) copy of this
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Triple<T1, T2, T3> copy() {
		Triple<T1, T2, T3> triple = new Triple<T1, T2, T3>();
		Class c1 = this.first.getClass(),
			  c2 = this.second.getClass(),
			  c3 = this.third.getClass();
		if (Table.class.equals(c1)) {
			triple.first = (T1)((Table)this.first).copy();
		} else if (Pair.class.equals(c1)) {
			triple.first = (T1)((Pair)this.first).copy();
		} else if (Triple.class.equals(c1)) {
			triple.first = (T1)((Triple)this.first).copy();
		} else if (Quadruple.class.equals(c1)) {
			triple.first = (T1)((Quadruple)this.first).copy();
		} else {
			triple.first = this.first;
		}
		if (Table.class.equals(c2)) {
			triple.second = (T2)((Table)this.second).copy();
		} else if (Pair.class.equals(c2)) {
			triple.second = (T2)((Pair)this.second).copy();
		} else if (Triple.class.equals(c2)) {
			triple.second = (T2)((Triple)this.second).copy();
		} else if (Quadruple.class.equals(c2)) {
			triple.second = (T2)((Quadruple)this.second).copy();
		} else {
			triple.second = this.second;
		}
		if (Table.class.equals(c3)) {
			triple.third = (T3)((Table)this.third).copy();
		} else if (Pair.class.equals(c3)) {
			triple.third = (T3)((Pair)this.third).copy();
		} else if (Triple.class.equals(c3)) {
			triple.third = (T3)((Triple)this.third).copy();
		} else if (Quadruple.class.equals(c3)) {
			triple.third = (T3)((Quadruple)this.third).copy();
		} else {
			triple.third = this.third;
		}
		return triple;
	}
}

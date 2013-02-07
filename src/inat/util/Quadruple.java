package inat.util;

public class Quadruple<T1, T2, T3, T4> {
	public T1 first;
	public T2 second;
	public T3 third;
	public T4 fourth;
	
	public Quadruple() {
	}
	
	public Quadruple(T1 first, T2 second, T3 third, T4 fourth) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.fourth = fourth;
	}
	

	/**
	 * returns a (deep) copy of this
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Quadruple<T1, T2, T3, T4> copy() {
		Quadruple<T1, T2, T3, T4> quadruple = new Quadruple<T1, T2, T3, T4>();
		Class c1 = this.first.getClass(),
			  c2 = this.second.getClass(),
			  c3 = this.third.getClass(),
			  c4 = this.fourth.getClass();
		if (Table.class.equals(c1)) {
			quadruple.first = (T1)((Table)this.first).copy();
		} else if (Pair.class.equals(c1)) {
			quadruple.first = (T1)((Pair)this.first).copy();
		} else if (Triple.class.equals(c1)) {
			quadruple.first = (T1)((Triple)this.first).copy();
		} else if (Quadruple.class.equals(c1)) {
			quadruple.first = (T1)((Quadruple)this.first).copy();
		} else {
			quadruple.first = this.first;
		}
		if (Table.class.equals(c2)) {
			quadruple.second = (T2)((Table)this.second).copy();
		} else if (Pair.class.equals(c2)) {
			quadruple.second = (T2)((Pair)this.second).copy();
		} else if (Triple.class.equals(c2)) {
			quadruple.second = (T2)((Triple)this.second).copy();
		} else if (Quadruple.class.equals(c2)) {
			quadruple.second = (T2)((Quadruple)this.second).copy();
		} else {
			quadruple.second = this.second;
		}
		if (Table.class.equals(c3)) {
			quadruple.third = (T3)((Table)this.third).copy();
		} else if (Pair.class.equals(c3)) {
			quadruple.third = (T3)((Pair)this.third).copy();
		} else if (Triple.class.equals(c3)) {
			quadruple.third = (T3)((Triple)this.third).copy();
		} else if (Quadruple.class.equals(c3)) {
			quadruple.third = (T3)((Quadruple)this.third).copy();
		} else {
			quadruple.third = this.third;
		}
		if (Table.class.equals(c4)) {
			quadruple.fourth = (T4)((Table)this.fourth).copy();
		} else if (Pair.class.equals(c4)) {
			quadruple.fourth = (T4)((Pair)this.fourth).copy();
		} else if (Triple.class.equals(c4)) {
			quadruple.fourth = (T4)((Triple)this.fourth).copy();
		} else if (Quadruple.class.equals(c4)) {
			quadruple.fourth = (T4)((Quadruple)this.fourth).copy();
		} else {
			quadruple.fourth = this.fourth;
		}
		return quadruple;
	}

}

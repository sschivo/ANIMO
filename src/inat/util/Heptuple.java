package inat.util;

public class Heptuple<T1, T2, T3, T4, T5, T6, T7> {
	public Quadruple<T1, T2, T3, T4> first4;
	public Triple<T5, T6, T7> second3;
	public T1 first;
	public T2 second;
	public T3 third;
	public T4 fourth;
	public T5 fifth;
	public T6 sixth;
	public T7 seventh;
	
	public Heptuple() {
	}
	
	public Heptuple(Quadruple<T1, T2, T3, T4> first4, Triple<T5, T6, T7> second3) {
		this.first4 = first4;
		first = first4.first;
		second = first4.second;
		third = first4.third;
		fourth = first4.fourth;
		this.second3 = second3;
		fifth = second3.first;
		sixth = second3.second;
		seventh = second3.third;
	}
	
	public Heptuple(T1 first, T2 second, T3 third, T4 fourth, T5 fifth, T6 sixth, T7 seventh) {
		this(new Quadruple<T1, T2, T3, T4>(first, second, third, fourth), new Triple<T5, T6, T7>(fifth, sixth, seventh));
	}
	
	
	/**
	 * returns a (deep) copy of this
	 * @return
	 */
	public Heptuple<T1, T2, T3, T4, T5, T6, T7> copy() {
		Quadruple<T1, T2, T3, T4> firstCopy = first4.copy();
		Triple<T5, T6, T7> secondCopy = second3.copy();
		return new Heptuple<T1, T2, T3, T4, T5, T6, T7>(firstCopy, secondCopy);
	}

}

package app;

import ac.SourceModel;

public class PriorValuePixelModel implements SourceModel<Integer> {
	
	private Integer[] _differences;
	private int[] _counts;
	private int _total_count;
	
	public PriorValuePixelModel(Integer[] differences, int[] counts) {
		assert differences != null;
		assert differences.length > 1;
		if (counts != null) {
			assert differences.length == counts.length;			
		} else {
			counts = new int[differences.length];			
			for (int i=0; i<counts.length; i++) {
				counts[i] = 1; //TODO: or 0 here? Answer: No if using the existing ACEncoding, 
				//because we need some base value to start calculating the CDFs
				//However, if we use an escaper, 0 is allowed
			}
			//System.out.println("Created a model with counts length " + counts.length);
		}

		_total_count = 0;
		for (int i=0; i<differences.length; i++) {
			assert differences[i] != null;
			assert counts[i] >= 0;
			_total_count += counts[i];
		}
		
		_differences = differences.clone();
		_counts = counts.clone();
	}
	
	public PriorValuePixelModel(Integer[] differences) {
		this(differences, null);
	}
	
	public void updateCount(int index) {
		_counts[index + 255 ]++;
		_total_count++;
	}

	@Override
	public int size() {
		return _differences.length;
	}

	@Override
	public Integer get(int diff) {
		int index = diff ;
		assert index >= 0 && index < size();
		
		return _differences[index];
	}

	@Override
	public double cdfLow(int index) {
		assert index >= 0 && index < size();

		int cumulative_count = 0;

		for (int i=0; i < index; i++) {
			cumulative_count += _counts[i];
		}
		
		return (1.0 * cumulative_count) / (1.0 * _total_count);
	}
}

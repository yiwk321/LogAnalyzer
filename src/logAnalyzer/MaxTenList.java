package logAnalyzer;

import java.util.ArrayList;
import java.util.List;

public class MaxTenList {
	protected List<Long> list = new ArrayList<>();
	
	public void add(long num) {
		for (int i = list.size()-1; i >= 0; i--) {
			if (num < list.get(i)) {
				list.add(i+1, num);
				if (list.size() > 10) {
					list.remove(10);
				}
				return;
			}
		}
		list.add(0, num);
		if (list.size() > 10) {
			list.remove(10);
		}
	}
	
	public long get(int i) {
		return list.get(i);
	}
	
	public int size() {
		return list.size();
	}
}

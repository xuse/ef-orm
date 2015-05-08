package jef.tools.algorithm;

public class Dijkstra {
	public static void main(String[] args) {
		new Dijkstra().use();
	}

	public void use() {
		new Dijkstra().dijkstra(0, a, dist, prev);
		for (int i = 0; i < dist.length; i++) {
			System.out.print(dist[i] + "  ");
		}
	}

	// 单元最短路径问题的Dijkstra算法
	public void dijkstra(int v, float[][] a, float[] dist, int[] prev) {
		int n = dist.length - 1;
		if (v < 0 || v > n - 1)
			return;
		boolean[] s = new boolean[n + 1];
		// 初始化
		for (int i = 1; i <= n; i++) {
			dist[i] = a[v][i];
			s[i] = false;
			if (dist[i] == Float.MAX_VALUE) {
				prev[i] = 0;
			} else {
				prev[i] = v;
			}
		}
		dist[v] = 0;
		s[v] = true;
		for (int i = 1; i < n; i++) {
			float temp = Float.MAX_VALUE;
			int u = v;
			for (int j = 1; j <= n; j++) {
				if ((!s[j]) && (dist[j] < temp)) {
					u = j;
					temp = dist[j];
				}
			}
			s[u] = true; // 找到了第一个并入S的节点
			for (int j = 1; j <= n; j++) {
				if ((!s[j]) && (a[u][j] < Float.MAX_VALUE)) {
					float newdist = dist[u] + a[u][j];
					if (newdist < dist[j]) {
						// dist[j] 减少
						dist[j] = newdist;
						prev[j] = u;
					}
				}
			}
		}
	}

	private float[][] a = { { 0, 10, max, 30, 100 }, { max, 0, 50, max, max }, { max, max, 0, max, 10 }, { max, max, 20, 0, 60 }, { max, max, max, max, 0 } };
	private float[] dist = new float[5];
	private int[] prev = new int[5];
	public static final float max = Float.MAX_VALUE;
}
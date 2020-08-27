package ginrummy;
import java.io.FileWriter;
import java.util.HashMap;

public class Game {

	public static void main(String[] args) {
		//GinRummyGame.setPlayVerbose(true);
		//new GinRummyGame(new AdvancedGinRummyPlayer(), new SimpleGinRummyPlayer()).play();
		//System.out.println(play(new AdvancedGinRummyPlayer(), new SimpleGinRummyPlayer(), 100000));
		//System.out.println("THRESHOLD 10 vs THRESHOLD 9: " + play(new AdvancedGinRummyPlayer(10),new AdvancedGinRummyPlayer(9),100000));
		//roundCsv("roundData.csv",new SimpleGinRummyPlayer(),500);
		//System.out.println(play(new DynamicGinRummyPlayer(),new SimpleGinRummyPlayer(),100000));
		/*try{
			FileWriter f = new FileWriter("deviationData.csv");
			System.out.println(playDynamic(new DynamicGinRummyPlayer(), new AdvancedGinRummyPlayer(), 10000 , null));
		}catch(Exception e) {
			System.out.println(e);
		}*/
		System.out.println(playDynamic(new DynamicGinRummyPlayer(), new SimpleGinRummyPlayer(), 100000, null));
	}
	
	public static float play(GinRummyPlayer p0, GinRummyPlayer p1, int x) {
		int p0_wins = 0;
		GinRummyGame.setPlayVerbose(false);
		//GinRummyGame.setPlayVerbose(true);
		for (int i = 0; i < x; ++i) {
			int winner = new GinRummyGame(p0, p1).play();
			if (winner == 0)
				++p0_wins;
		}
		return p0_wins;
	}
	
	public static float playDynamic(DynamicGinRummyPlayer d, GinRummyPlayer p, int x, FileWriter f) {
		float total = 0;
		if (f != null) {
			try {
				f.append("Max value deviation\n");
				total = play(d,p,x);
				f.flush();
				f.close();
			}catch(Exception e) {
				System.out.println(e);
			}
		}else {
			total = play(d,p,x);
		}
		
		System.out.println("Threshold: " + d.getKnockThreshold());
		HashMap<Integer, Double[]> netThresholds = d.getThresholds();
		for (Integer i : netThresholds.keySet()) {
				Double[] dou = netThresholds.get(i);
				System.out.print(i + ":[ " + dou[0] + ", " + dou[1] + " ] ");
			}
			System.out.println();
		return total;
		
	}
	
	public static void roundCsv(String fileName, GinRummyPlayer p1, int x) {
		try {
			FileWriter csvWriter = new FileWriter(fileName);
			csvWriter.append("Game");
			csvWriter.append(",");
			csvWriter.append("Round");
			csvWriter.append(",");
			csvWriter.append("Deadwood");
			csvWriter.append("\n");
			
			play(new SimpleGRCPlayerRoundData(csvWriter), p1,x);
			csvWriter.flush();
			csvWriter.close();
			} catch(Exception e) {
				System.out.println(e.toString());
			}
	}
	
}

import java.io.FileWriter;

public class Game {

	public static void main(String[] args) {
		//GinRummyGame.setPlayVerbose(true);
		//new GinRummyGame(new AdvancedGinRummyPlayer(), new SimpleGinRummyPlayer()).play();
		//System.out.println(play(new AdvancedGinRummyPlayer(), new SimpleGinRummyPlayer(), 100000));
		roundCsv("roundData.csv",new SimpleGinRummyPlayer(),1);
		
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
	
	public static void roundCsv(String fileName, GinRummyPlayer p1, int x) {
		try {
			FileWriter csvWriter = new FileWriter(fileName);
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

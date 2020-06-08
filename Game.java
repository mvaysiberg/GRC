
public class Game {

	public static void main(String[] args) {
		//GinRummyGame.setPlayVerbose(true);
		//new GinRummyGame(new AdvancedGinRummyPlayer(), new SimpleGinRummyPlayer()).play();
		System.out.println(play(new AdvancedGinRummyPlayer(), new SimpleGinRummyPlayer(), 10000));
	}
	
	public static float play(GinRummyPlayer p0, GinRummyPlayer p1, int x) {
		int p0_wins = 0;
		GinRummyGame.setPlayVerbose(false);
		for (int i = 0; i < x; ++i) {
			int winner = new GinRummyGame(p0, p1).play();
			if (winner == 0)
				++p0_wins;
		}
		return (float)p0_wins /x;
	}
}

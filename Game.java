
public class Game {

	public static void main(String[] args) {
		GinRummyGame.setPlayVerbose(true);
		new GinRummyGame(new AdvancedGinRummyPlayer(), new SimpleGinRummyPlayer()).play();

	}

}

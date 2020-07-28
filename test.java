import java.util.HashSet;

import ginrummy.AdvancedGinRummyPlayer;
import ginrummy.Card;

public class test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*AdvancedGinRummyPlayer p1 = new AdvancedGinRummyPlayer();
		String[] strHand = {"TD", "4H", "2C", "QC", "AH", "9H", "AS", "TS", "2S", "QH"};
		String[] hand2 = {"2D", "5C", "6H", "6S", "7D", "7C", "9C", "TC", "QD", "KD"};
		Card[] hand = new Card[hand2.length];
		for (int i = 0; i < strHand.length; ++i) {
			hand[i] = Card.strCardMap.get(strHand[i]);
		}
		p1.startGame(0, 0, hand);
		System.out.println(p1.getHand());
		System.out.println(p1.getWantCards());
		System.out.println("Potential Run" + p1.getPotentialRun());
		HashSet<Card> s = new HashSet<Card>(p1.getPotentialSet());
		System.out.println("Potential Set" + s);
		Card c = p1.getDiscard();
		System.out.println(c + " " + s.contains(c));
		System.out.println(c.equals(new Card(c.rank,c.suit)));
		*/
		Double d[] = {-123.56, -9.78};
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < d.length; ++ i) {
			if (d[i] > max)
				max = d[i];
		}
		System.out.println(max);
	}

}

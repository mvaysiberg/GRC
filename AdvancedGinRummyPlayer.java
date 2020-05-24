import java.util.ArrayList;
import java.util.HashSet;

public class AdvancedGinRummyPlayer implements GinRummyPlayer{
	private int playerNum;
	private int startingPlayerNum;
	private int randomSetSize;
	private ArrayList<Card> hand;
	private ArrayList<Card> opponentHand;
	private HashSet<Card> seenCards;
	private Card lastDrawnCard;
	@Override
	public void startGame(int playerNum, int startingPlayerNum, Card[] cards) {
		// TODO Auto-generated method stub
		this.playerNum = playerNum;
		this.startingPlayerNum = startingPlayerNum;
		randomSetSize = 31;
		hand = new ArrayList<Card>();
		seenCards = new HashSet<Card>();
		
		for (Card c: cards) { //hand is  sorted, and all cards in hand added to seen hashset
			seenCards.add(c);
			if(hand.isEmpty())
				hand.add(c);
			else {
				int left = 0;
				int right = hand.size() -1;
				int middle = -1;
				while (left <= right) {
					middle = (left + right)/2;
					if (hand.get(middle).rank == c.rank) 
						break;
					else if (hand.get(middle).rank < c.rank)
						left = middle + 1;
					else
						right = middle -1;
				}
				
					hand.add(middle, c);
			}
		}
		
		opponentHand = new ArrayList<Card>();
		
			
	}

	@Override
	public boolean willDrawFaceUpCard(Card card) {
		// TODO Auto-generated method stub
		//logic to decide whether to take the card from the discarded set
		
		//return true;
		randomSetSize--;
		return false;
	}

	@Override
	public void reportDraw(int playerNum, Card drawnCard) {
		// TODO Auto-generated method stub
		if (playerNum == this.playerNum) { //Reports what we drew
			seenCards.add(drawnCard);
			lastDrawnCard = drawnCard;
			//drawncard is inserted into hand in the proper sorted position
			int left = 0;
			int right = hand.size() -1;
			int middle = -1;
			while (left <= right) {
				middle = (left + right)/2;
				if (hand.get(middle).rank == drawnCard.rank) 
					break;
				else if (hand.get(middle).rank < drawnCard.rank)
					left = middle + 1;
				else
					right = middle -1;
			}
			
				hand.add(middle, drawnCard);
			
		}else {
			if (drawnCard == null) { //opponent drew from random set, no knowledge of what the card is
				randomSetSize--;
			}else { //opponent has picked up the card from the discarded set, we have already seen this card before
				if (opponentHand.isEmpty())
					opponentHand.add(drawnCard);
				else {
					int left = 0;
					int right = opponentHand.size() -1;
					int middle = -1;
					while (left <= right) {
						middle = (left + right)/2;
						if (opponentHand.get(middle).rank == drawnCard.rank) 
							break;
						else if (opponentHand.get(middle).rank < drawnCard.rank)
							left = middle + 1;
						else
							right = middle -1;
					}
					
					opponentHand.add(middle, drawnCard);
				}
				
			}
		}
	}

	@Override
	public Card getDiscard() {
		// TODO Auto-generated method stub
		//choose which card to discard, cannot be lastDrawnCard
		return null;
	}

	@Override
	public void reportDiscard(int playerNum, Card discardedCard) {
		// TODO Auto-generated method stub
		if (playerNum != this.playerNum) { //reports the card that the opponent discarded
			seenCards.add(discardedCard);
			if (opponentHand.contains(discardedCard))
				opponentHand.remove(discardedCard);
		} //update prediction model???
	}

	@Override
	public ArrayList<ArrayList<Card>> getFinalMelds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reportFinalMelds(int playerNum, ArrayList<ArrayList<Card>> melds) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reportScores(int[] scores) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reportLayoff(int playerNum, Card layoffCard, ArrayList<Card> opponentMeld) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reportFinalHand(int playerNum, ArrayList<Card> hand) {
		// TODO Auto-generated method stub
		
	}
	
}

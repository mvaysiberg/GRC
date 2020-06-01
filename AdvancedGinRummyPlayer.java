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
	private int deadWood;
	private ArrayList<ArrayList<ArrayList<Card>>> bestMelds;
	private HashSet<Card> wantCards;
	private HashSet<Card> potentialSet;
	private HashSet<Card> potentialRun;
	private boolean opponentKnocked;
	private ArrayList<ArrayList<Card>> opponentFinalMelds;
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
			insertSorted(c, hand);
		}
		
		lastDrawnCard = null;
		opponentHand = new ArrayList<Card>();
		updateMeldsDeadWood();
		updateWantCards();
		opponentKnocked = false;
	}

	@Override
	public boolean willDrawFaceUpCard(Card card) {
		// TODO Auto-generated method stub
		lastDrawnCard = null;
		//logic to decide whether to take the card from the discarded set
		if (!seenCards.contains(card)) //first turn
				seenCards.add(card);
		
		if (wantCards.contains(card)) //card will be added to a run/set
			return true;
		Card willDiscard = discard();
		if (GinRummyUtil.getDeadwoodPoints(willDiscard) - GinRummyUtil.getDeadwoodPoints(card) >= 5) 
			return true;
		else {
			randomSetSize--;
			return false;
		}
	}

	@Override
	public void reportDraw(int playerNum, Card drawnCard) {
		// TODO Auto-generated method stub
		if (drawnCard != null && !seenCards.contains(drawnCard))
			seenCards.add(drawnCard);
		if (playerNum == this.playerNum) { //Reports what we drew
			lastDrawnCard = drawnCard;
			//drawncard is inserted into hand in the proper sorted position
			insertSorted(drawnCard,hand);
			updateMeldsDeadWood();
		}else {
			if (drawnCard == null) { //opponent drew from random set, no knowledge of what the card is
				randomSetSize--;
			}else { //opponent has picked up the card from the discarded set, we have already seen this card before
				insertSorted(drawnCard,opponentHand);
			}
		}
	}

	@Override
	public Card getDiscard() {
		// TODO Auto-generated method stub
		//choose which card to discard, cannot be lastDrawnCard
		Card ret = discard();
		hand.remove(ret);
		updateMeldsDeadWood();
		updateWantCards();
		return ret;
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
		if (deadWood == 0) { //auto knocks when gin
			return bestMelds.get(0);
		}else if (opponentKnocked) { //need to test, very bug prone!!!
			if (bestMelds.isEmpty()) { //opponent knocked and we have no melds =(
				return null;
			}else { //opponent knocked and we have melds, choose one that maximizes layoffs
				ArrayList<ArrayList<Card>> minMelds = bestMelds.get(0);
				int minDeadWood = deadWood;
				for (ArrayList<ArrayList<Card>> meldSet: bestMelds) {
					ArrayList<Card> deadHand = new ArrayList<Card>(hand); //find all deadwood in hand
					for (ArrayList<Card> meld: meldSet) {
						for (Card c: meld) { //remove all melds from deadHand
							deadHand.remove(c);
						}
					}
					
					for (Card c: deadHand) { //lays off deadwood
						for (ArrayList<Card> opponentMeld: opponentFinalMelds) {
							opponentMeld.add(c);
							if (isSet(opponentMeld) || isRun(opponentMeld)) {
								deadHand.remove(c);
								break;
							}
						}
					}
					
					int curDeadWood = GinRummyUtil.getDeadwoodPoints(deadHand);
					if (curDeadWood < minDeadWood) {
						minDeadWood = curDeadWood;
						minMelds = meldSet;
					}
				}
				return minMelds;
			}
		} else if (deadWood <= 10 && !opponentKnocked) { //we knocked (knock as soon as possible basic strategy)
			//Jenny's part
			for (ArrayList<ArrayList<Card>> meldSet: bestMelds) {
				if (bestMelds.contains(opponentHand)) //check whether opponent face-up card they draw MATCH our sets/runs
					bestMelds.remove(index);//not sure how to remove that specific meld
				}
		} else {
			return null; //no one knocks
		}
	}

	@Override
	public void reportFinalMelds(int playerNum, ArrayList<ArrayList<Card>> melds) {
		// TODO Auto-generated method stub
		if (playerNum != this.playerNum) {
			opponentFinalMelds = melds;
			opponentKnocked = true;
		}
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
	private void insertSorted(Card c, ArrayList<Card> a) {
		if (a.isEmpty())
			a.add(c);
		else {
			int left = 0;
			int right = hand.size() -1;
			int middle = -1;
			while (left <= right) {
				middle = (left + right)/2;
				if (a.get(middle).rank == c.rank) 
					break;
				else if (hand.get(middle).rank < c.rank)
					left = middle + 1;
				else
					right = middle -1;
			}
				if (a.get(middle).rank < c.rank)
					a.add(middle + 1, c);
				else
					a.add(middle,c);
		}
	}
	private void updateMeldsDeadWood() {
		bestMelds = GinRummyUtil.cardsToBestMeldSets(hand);
		if (bestMelds.isEmpty())
			deadWood = GinRummyUtil.getDeadwoodPoints(hand);
		else
			deadWood = GinRummyUtil.getDeadwoodPoints(bestMelds.get(0), hand);
	}
	
	private void updateWantCards() {
		wantCards = new HashSet<Card>();
		potentialSet = new  HashSet<Card>();
		for (int i = 0; i < hand.size(); ++i) { //this for loop calculates what cards we want to add to sets (if we have 2 or 3 cards of the same rank, look for the last 1 or 2)
			int cardNum = hand.get(i).rank;
			int count = 0;
			HashSet<Integer> suits = new HashSet<Integer>();
			while (i < hand.size() && hand.get(i).rank == cardNum) {
				suits.add(hand.get(i).suit);
				count++;
				i++;
			}
			if (count == 2 || count == 3) {
				if (!suits.contains(0))
					wantCards.add(new Card(cardNum,0));
				if (!suits.contains(1))
					wantCards.add(new Card(cardNum,1));
				if (!suits.contains(2))
					wantCards.add(new Card(cardNum,2));
				if (!suits.contains(3))
					wantCards.add(new Card(cardNum,3));
			}
			if (count == 2) {
				for (Integer suit: suits) {
					potentialSet.add(new Card(cardNum,suit));
				}
			}
			i--;
		}
		potentialRun = new HashSet<Card>();
		for (int i = 0; i < hand.size(); ++i) { //this for loop calculates what cards we want to add to runs or form runs
			int suit = hand.get(i).suit;
			int startingRank = hand.get(i).rank;
			int x = i;
			int count = 0;
			ArrayList<Integer> ranks = new ArrayList<Integer>();
			while (x < hand.size() && (hand.get(x).rank == startingRank || hand.get(x).rank == startingRank + 1)) {
				if (ranks.isEmpty()) {
					ranks.add(hand.get(x).rank);
					count++;
				}else {
					if (hand.get(x).suit == suit && hand.get(x).rank == ranks.get(ranks.size() -1)+1) {
						count++;
						ranks.add(hand.get(x).rank);
						startingRank = hand.get(x).rank;
					}
				}
				x++;
			}
			if (count >= 2) {
				if (ranks.get(0) != 0) //the minimal card in a run is an ace
					wantCards.add(new Card(ranks.get(0)-1,suit));
				if (ranks.get(ranks.size() -1) != 12) //the maximal card in a run is a king
					wantCards.add(new Card(ranks.get(ranks.size()-1)+1,suit));
				if (count == 2) {
					for (Integer rank: ranks) {
						potentialRun.add(new Card(rank,suit));
					}
				}
			}else if (count == 1) { //handle the case when there is a card of the same suit in rank startingRank + 2
				startingRank = hand.get(i).rank;
				x = i;
				while (x < hand.size() && hand.get(x).rank <= startingRank + 2) {
					if (hand.get(x).suit == suit && hand.get(x).rank == startingRank + 2) {
						wantCards.add(new Card(startingRank + 1, suit));
						potentialRun.add(new Card(startingRank,suit));
						potentialRun.add(new Card(startingRank + 2, suit));
						break;
					}
					x++;
				}
			}
		}
	}
	public ArrayList<Card> getHand() { //returns hand for testing
		return hand;
	}
	public HashSet<Card> getWantCards(){ //returns  wantCards set for testing
		return wantCards;
	}
	public HashSet<Card> getPotentialSet(){ //returns potentialSet for testing
		return potentialSet;
	}
	public HashSet<Card> getPotentialRun(){ //returns potentialRun for testing
		return potentialRun;
	}
	private Card discard() { //gets card to discard from current hand
		ArrayList<Card> potentialDiscards = new ArrayList<Card>();
		for (Card handcard: hand) {
			boolean inMeld = false;
			for (ArrayList<ArrayList<Card>> melds: bestMelds) {
				for (ArrayList<Card> meld: melds) {
					for (Card c: meld) {
						if (handcard.rank == c.rank && handcard.suit == c.suit)
							inMeld = true;
					}
				}
			}
			if (!inMeld && (lastDrawnCard == null || (lastDrawnCard.rank != handcard.rank && lastDrawnCard.suit != handcard.suit)))
				potentialDiscards.add(handcard); //do not remove a card if it is in a set/run or if it is the last drawn card
		}
		
		//handle case when we gin
		if (potentialDiscards.isEmpty() && hand.size() == 11) { //size == 11 is redundant as we will automatically knock when gin, but added for clarity
			for (Card c: hand) { //need to check which card can be removed and still have deadwood = 0
				if (c.rank == lastDrawnCard.rank && c.suit == lastDrawnCard.suit)
					continue;
				else {
					ArrayList<Card> potentialGinHand = new ArrayList<Card>(hand);
					potentialGinHand.remove(c);
					ArrayList<ArrayList<Card>> curMelds = GinRummyUtil.cardsToBestMeldSets(potentialGinHand).get(0);
					if (GinRummyUtil.getDeadwoodPoints(curMelds,potentialGinHand) == 0)
						return c; //guaranteed to return as there must be one meld with > 3 cards
				}
			}
			return null; //will never be called, added so code can compile
		}else {
		
			ArrayList<Integer>deadWood = new ArrayList<Integer>();
			for (Card c: potentialDiscards) {
				deadWood.add(GinRummyUtil.getDeadwoodPoints(c));
			}
			int maxDeadDeadWood = 0;
			int maxPotentialMeldDeadWood = 0;
			ArrayList<Card> deadlist = new ArrayList<Card>();
			ArrayList<Card> meldlist = new ArrayList<Card>();
			for (int i = 0; i < deadWood.size(); ++i) { //find max deadwood values for dead deadwood and for potential sets/runs
				if (potentialSet.contains(potentialDiscards.get(i)) || potentialRun.contains(potentialDiscards.get(i))) {
					if (deadWood.get(i) > maxPotentialMeldDeadWood) {
						maxPotentialMeldDeadWood = deadWood.get(i);
						meldlist.clear();
						meldlist.add(potentialDiscards.get(i));
					}else if (deadWood.get(i) == maxPotentialMeldDeadWood) {
						meldlist.add(potentialDiscards.get(i));
					}
				}else {
					if (deadWood.get(i) > maxDeadDeadWood) {
						maxDeadDeadWood = deadWood.get(i);
						deadlist.clear();
						deadlist.add(potentialDiscards.get(i));
					}else if (deadWood.get(i) == maxDeadDeadWood) {
						deadlist.add(potentialDiscards.get(i));
					}
				}
			}
			ArrayList<Card> willDiscard;
			if (maxPotentialMeldDeadWood - maxDeadDeadWood >= 6) { //case when we discard the potential meld/set, we can find the optimal threshold later
				willDiscard = meldlist;
			}else { //case when we discard largest from dead deadwood
				willDiscard = deadlist;
			}
			return willDiscard.get(0); //may want to choose which specific card depending on opponent's hand later
		}
	}	

	private boolean isSet(ArrayList<Card> cards) {
		if (cards.size() < 3)
			return false;
		int rank = cards.get(0).rank;
		for (Card c: cards) {
			if (c.rank != rank)
				return false;
		}
		return true;
	}
	private boolean isRun(ArrayList<Card> cards) {
		if (cards.size() < 3)
			return false;
		ArrayList<Card> cardsSorted = new ArrayList<Card>();
		for (Card c: cards) {
			insertSorted(c, cardsSorted);
		}
		int suit = cardsSorted.get(0).suit;
		for (int i = 1; i < cardsSorted.size(); ++i) {
			if (cards.get(i).suit != suit || cards.get(i).rank != cards.get(i-1).rank + 1)
				return false;
		}
		return true;
	}
	
}

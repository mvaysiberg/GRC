package ginrummy;
import java.util.ArrayList;
import java.math.*;
import java.util.HashSet;

public class AdvancedGinRummyPlayer implements GinRummyPlayer{
	private int playerNum;
	private ArrayList<int[]> gameScores;
	private int startingPlayerNum;
	private int randomSetSize;
	private int roundNum;
	private ArrayList<Card> hand;
	private ArrayList<Card> opponentHand;
	private HashSet<Card> seenCards;
	private Card lastDrawnCard;
	private boolean tookFaceup;
	private int deadWood;
	private ArrayList<ArrayList<ArrayList<Card>>> bestMelds;
	private HashSet<Card> wantCards;
	private HashSet<Card> potentialSet;
	private HashSet<Card> potentialRun;
	private HashSet<Card> potentialHardRun;
	private HashSet<Card> potentialHardSet;
	private boolean opponentKnocked;
	private HashSet<Card> sets;
	private ArrayList<ArrayList<Card>> opponentFinalMelds;
	private ArrayList<Card> opponentDiscards;
	private int KNOCK_THRESHOLD;
	//private int numMelds;
	//private int numPotentials;
	public AdvancedGinRummyPlayer() {
		KNOCK_THRESHOLD = 9;
		gameScores = new ArrayList<int[]>();
	}
	public AdvancedGinRummyPlayer(int knockThreshold) { //made for the sole purpose of testing knock algorithm, delete in final version
		KNOCK_THRESHOLD = knockThreshold;
		gameScores = new ArrayList<int[]>();
	}
	
	@Override
	public void startGame(int playerNum, int startingPlayerNum, Card[] cards) {
		// TODO Auto-generated method stub
		tookFaceup = false;
		this.playerNum = playerNum;
		this.startingPlayerNum = startingPlayerNum;
		randomSetSize = 31;
		hand = new ArrayList<Card>();
		seenCards = new HashSet<Card>();
		opponentDiscards = new ArrayList<Card>();
		for (Card c: cards) { //hand is  sorted, and all cards in hand added to seen hashset
			seenCards.add(c);
			insertSorted(c, hand);
		}
		roundNum = 0;
		lastDrawnCard = null;
		opponentHand = new ArrayList<Card>();
		updateMeldsDeadWood();
		updateWantCards(hand);
		opponentKnocked = false;
	}

	@Override
	public boolean willDrawFaceUpCard(Card card) {
		// TODO Auto-generated method stub
		updateMeldsDeadWood();
		updateWantCards(hand);
		lastDrawnCard = null;
		//logic to decide whether to take the card from the discarded set
		if (!seenCards.contains(card)) //first turn
				seenCards.add(card);
		if (deadWood == 0) //will draw a card and discard it immediately to gin
			return false;
		if (hashSetContains(wantCards,card)) { //card will be added to a run/set
			tookFaceup = true;
			return true;
		}
		ArrayList<Card> tempHand = new ArrayList<Card>(hand);
		insertSorted(card,tempHand);
		lastDrawnCard = card; //pretend that we drew the card for the sake of the algorithm
		updateWantCards(tempHand);//this will be undone after report draw anyway
		Card willDiscard = discard(tempHand);
		lastDrawnCard = null;
		if (deadWood <= 26) {
			if (willDiscard != null && GinRummyUtil.getDeadwoodPoints(willDiscard) - GinRummyUtil.getDeadwoodPoints(card) >= 7) {
				tookFaceup = true;
				return true;
			}else {
				randomSetSize--;
				tookFaceup = false;
				return false;
			}
		}else {
			if (willDiscard != null && GinRummyUtil.getDeadwoodPoints(willDiscard) - GinRummyUtil.getDeadwoodPoints(card) >=7 &&(hashSetContains(potentialHardSet,card) || hashSetContains(potentialHardRun,card))){
				tookFaceup = true;
				return true;
			}
			else if (willDiscard != null && GinRummyUtil.getDeadwoodPoints(willDiscard) - GinRummyUtil.getDeadwoodPoints(card) >= 5 && (hashSetContains(potentialSet, card) || hashSetContains(potentialRun,card))) {
				tookFaceup = true;
				return true;
			}else {
				randomSetSize--;
				tookFaceup = false;
				return false;
			}
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
			updateWantCards(hand);
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
		Card ret = discard(hand);
		hand.remove(ret);
		updateMeldsDeadWood();
		updateWantCards(hand);
		return ret;
	}

	@Override
	public void reportDiscard(int playerNum, Card discardedCard) {
		// TODO Auto-generated method stub
		if (playerNum != this.playerNum) { //reports the card that the opponent discarded
			seenCards.add(discardedCard);
			insertSorted(discardedCard, opponentDiscards); //opponent discarded cards are sorted?
			if (opponentHand.contains(discardedCard))
				opponentHand.remove(discardedCard);
		} //update prediction model???
	}

	@Override
	public ArrayList<ArrayList<Card>> getFinalMelds() {
		// TODO Auto-generated method stub
		++roundNum;
		if (deadWood == 0) { //auto knocks when gin
			return bestMelds.get(0);
		}else if (opponentKnocked) { //need to test, very bug prone!!!
			if (bestMelds.isEmpty()) { //opponent knocked and we have no melds =(
				return new ArrayList<ArrayList<Card>>();
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
					ArrayList<Card> tempDeadHand = new ArrayList<Card>(deadHand);
					for (Card c: deadHand) { //lays off deadwood
						for (ArrayList<Card> opponentMeld: opponentFinalMelds) {
							ArrayList<Card> tempOpponentMeld = new ArrayList<Card>(opponentMeld);
							tempOpponentMeld.add(c);
							if (isSet(tempOpponentMeld) || isRun(tempOpponentMeld)) {
								tempDeadHand.remove(c);
								break;
							}
						}
					}
					
					int curDeadWood = GinRummyUtil.getDeadwoodPoints(tempDeadHand);
					if (curDeadWood < minDeadWood) {
						minDeadWood = curDeadWood;
						minMelds = meldSet;
					}
				}
				return minMelds;
			}
		} else if (deadWood <= dWoodFunc(roundNum) -KNOCK_THRESHOLD && deadWood <= GinRummyUtil.MAX_DEADWOOD && !opponentKnocked) { //we knocked 
			ArrayList<ArrayList<Card>> bestMeld = bestMelds.get(0);
			int opponentMaxDeadwood = 0;
			for (ArrayList<ArrayList<Card>> meldSet: bestMelds) {
				ArrayList<Card> opponentLayoffHand = new ArrayList<Card>(opponentHand);
				for (Card c: opponentHand) {
					for (ArrayList<Card> meld: meldSet) {
						ArrayList<Card> tempMeld = new ArrayList<Card>(meld);
						tempMeld.add(c);
						if (isSet(tempMeld) || isRun(tempMeld)) { //checks if opponent's card can be added to one of our melds
							opponentLayoffHand.remove(c);
						}
					}
				}
				int curDeadwood = GinRummyUtil.getDeadwoodPoints(opponentLayoffHand);
				if (curDeadwood > opponentMaxDeadwood) { //return the hand that will make the opponent have the most amount of deadwood
					bestMeld = meldSet;
					opponentMaxDeadwood = curDeadwood;
				}
			}
			return bestMeld;
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
		if (scores[0] >= GinRummyUtil.GOAL_SCORE || scores[1] >= GinRummyUtil.GOAL_SCORE) { //only add score after the end of the game
			gameScores.add(scores);
			//System.out.println(scores[0] + " " + scores[1]);
		}
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
		if (a.isEmpty()) {
			a.add(c);
		}else {
			int left = 0;
			int right = a.size() -1;
			int middle = -1;
			while (left <= right) {
				middle = (left + right)/2;
				if (a.get(middle).rank == c.rank) 
					break;
				else if (a.get(middle).rank < c.rank)
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
		sets = new HashSet<Card>();
		bestMelds = GinRummyUtil.cardsToBestMeldSets(hand);
		if (bestMelds.isEmpty())
			deadWood = GinRummyUtil.getDeadwoodPoints(hand);
		else {
			deadWood = GinRummyUtil.getDeadwoodPoints(bestMelds.get(0), hand);
			for (ArrayList<ArrayList<Card>> melds: bestMelds) {
				for (ArrayList<Card> meld: melds) {
					if (isSet(meld)) {
						for (Card c: meld) {
							sets.add(c);
						}
					}
				}
			}
			//numMelds = bestMelds.get(0).size();
		}
	}
	
	private void updateWantCards(ArrayList<Card> h) {
		wantCards = new HashSet<Card>();
		potentialSet = new  HashSet<Card>();
		potentialHardSet = new HashSet<Card>();
		for (int i = 0; i < h.size(); ++i) { //this for loop calculates what cards we want to add to sets (if we have 2 or 3 cards of the same rank, look for the last 1 or 2)
			int cardNum = h.get(i).rank;
			int count = 0;
			HashSet<Integer> suits = new HashSet<Integer>();
			while (i < h.size() && h.get(i).rank == cardNum) {
				suits.add(h.get(i).suit);
				count++;
				i++;
			}
			ArrayList<Card> possibleS = new ArrayList<Card>();
			if (count == 2 || count == 3) {
				if (!suits.contains(0)) {
					wantCards.add(new Card(cardNum,0));
					possibleS.add(new Card(cardNum,0));
				}if (!suits.contains(1)) {
					wantCards.add(new Card(cardNum,1));
					possibleS.add(new Card(cardNum,1));
				}if (!suits.contains(2)) {
					wantCards.add(new Card(cardNum,2));
					possibleS.add(new Card(cardNum,2));
				}if (!suits.contains(3)) {
					wantCards.add(new Card(cardNum,3));
					possibleS.add(new Card(cardNum,3));
				}
			}
			if (count == 2 && isPossibleMeld(possibleS)) {
				for (Integer suit: suits) {
					if (!hashSetContains(potentialSet,new Card(cardNum,suit)))
						potentialSet.add(new Card(cardNum,suit));
					if (isHardMeld(possibleS))//create another condition for normal set once discard logic is added
						potentialHardSet.add(new Card(cardNum,suit));
				}
			}
			i--;
		}
		ArrayList<Card> newHand = new ArrayList<Card>(); //create a new hand without sets so that avoid trying to form runs with current sets
		for (Card c: h) {
			if (!hashSetContains(sets,c))
				newHand.add(c);
		}
		potentialRun = new HashSet<Card>();
		potentialHardRun = new HashSet<Card>();
		for (int i = 0; i < newHand.size(); ++i) { //this for loop calculates what cards we want to add to runs or form runs
			int suit = newHand.get(i).suit;
			int startingRank = newHand.get(i).rank;
			int x = i;
			int count = 0;
			ArrayList<Integer> ranks = new ArrayList<Integer>();
			if (hashSetContains(potentialRun,newHand.get(i))) //do not double count the same potential run
				continue;
			while (x < newHand.size() && (newHand.get(x).rank == startingRank || newHand.get(x).rank == startingRank + 1)) {
				if (ranks.isEmpty()) {
					ranks.add(newHand.get(x).rank);
					count++;
				}else {
					if (newHand.get(x).suit == suit && newHand.get(x).rank == ranks.get(ranks.size() -1)+1) {
						count++;
						ranks.add(newHand.get(x).rank);
						startingRank = newHand.get(x).rank;
					}
				}
				x++;
			}
			if (count >= 2) {
				ArrayList<Card> possibleR = new ArrayList<Card>();
				if (ranks.get(0) != 0) {//the minimal card in a run is an ace
					wantCards.add(new Card(ranks.get(0)-1,suit));
					possibleR.add(new Card(ranks.get(0)-1,suit));
				}if (ranks.get(ranks.size() -1) != 12) { //the maximal card in a run is a king
					wantCards.add(new Card(ranks.get(ranks.size()-1)+1,suit));
					possibleR.add(new Card(ranks.get(ranks.size()-1)+1,suit));
				}if (count == 2 && isPossibleMeld(possibleR)) {
					for (Integer rank: ranks) {
						potentialRun.add(new Card(rank,suit));
						if (isHardMeld(possibleR)) //create another condition for normal run once discard logic is added
							potentialHardRun.add(new Card(rank,suit));
					}
					//++numPotentials; //this is a potential run
				}
			}else if (count == 1) { //handle the case when there is a card of the same suit in rank startingRank + 2
				startingRank = newHand.get(i).rank;
				x = i;
				while (x < newHand.size() && newHand.get(x).rank <= startingRank + 2) {
					if (compareCards(newHand.get(x), new Card(startingRank +2,suit))) {
						wantCards.add(new Card(startingRank + 1, suit));
						if (!hashSetContains(seenCards,new Card(startingRank+1,suit))) {
							potentialRun.add(new Card(startingRank,suit)); //can comment out these lines once added new logic in discard algorithm
							potentialRun.add(new Card(startingRank + 2, suit));//
							potentialHardRun.add(new Card(startingRank,suit));
							potentialHardRun.add(new Card(startingRank + 2, suit));
						}
						//++numPotentials; //this is a potential run 
						break;
					}
					x++;
				}
			}
		}
		//numPotentials += numPotentialSets(potentialSet);
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
	private Card discard(ArrayList<Card> h) { //gets card to discard from current hand
		updateMeldsDeadWood();
		ArrayList<Card> potentialDiscards = new ArrayList<Card>();
		for (Card handcard: h) {
			boolean inMeld = false;
			if (!bestMelds.isEmpty()) {
				for (ArrayList<Card> meld: bestMelds.get(0)) {
					for (Card c: meld) {
						if (compareCards(handcard, c))
							inMeld = true;
					}
				}
			}
			if (!inMeld && (!tookFaceup ||lastDrawnCard == null || !compareCards(lastDrawnCard,handcard)))
				potentialDiscards.add(handcard); //do not remove a card if it is in a set/run or if it is the last drawn card
		}
		
		//handle case when we gin
		if (potentialDiscards.isEmpty() &&  deadWood == 0) { //size == 11 is redundant as we will automatically knock when gin, but added for clarity
			for (Card c: h) { //need to check which card can be removed and still have deadwood = 0
				if (lastDrawnCard == null || compareCards(c,lastDrawnCard))
					continue;
				else {
					ArrayList<Card> potentialGinHand = new ArrayList<Card>(h);
					potentialGinHand.remove(c);
					ArrayList<ArrayList<Card>> curMelds = GinRummyUtil.cardsToBestMeldSets(potentialGinHand).get(0);
					if (GinRummyUtil.getDeadwoodPoints(curMelds,potentialGinHand) == 0)
						return c; //guaranteed to return as there must be one meld with > 3 cards
				}
			}
			return null; //will never be called, added so code can compile
		}
		
		int maxUnmatchedDwood = -1;
		int maxUnmatchedPotentials  = -1;
		
		ArrayList<Card> potentials = new ArrayList<Card>();
		ArrayList<Card> dead = new ArrayList<Card>();
		for (Card c: potentialDiscards) {
			if (hashSetContains(potentialSet,c) || hashSetContains(potentialRun,c))
				insertSorted(c, potentials);
			else
				insertSorted(c,dead);
		}
		
		int pIndex = potentials.size() -1;
		int dIndex = dead.size() -1;
		
		while (pIndex >= 0) { //find the greatest potential set/run that does not match the opponent's hand
			if (!matches(potentials.get(pIndex),opponentHand))
				break;
			--pIndex;
		}
		
		while (dIndex >= 0) { //find the greatest dead deadwood that does not match the opponent's hand
			if (!matches(dead.get(dIndex),opponentHand))
				break;
			--dIndex;
		}
		
		if (dIndex >= 0) 
			maxUnmatchedDwood  = GinRummyUtil.getDeadwoodPoints(dead.get(dIndex));
		if (pIndex >= 0)
			maxUnmatchedPotentials  = GinRummyUtil.getDeadwoodPoints(potentials.get(pIndex));
		
		ArrayList<Integer>deadWood = new ArrayList<Integer>();
		for (Card c: potentialDiscards) {
			deadWood.add(GinRummyUtil.getDeadwoodPoints(c));
		}
		//
		//System.out.println("Discards" + potentialDiscards);
		//System.out.println("Deadwood" + deadWood);
		//System.out.println("Hand" + hand);
		//
		int maxMatchedDwood = 0;
		int maxMatchedPotentials = 0;
		ArrayList<Card> deadlist = new ArrayList<Card>();
		ArrayList<Card> meldlist = new ArrayList<Card>();
		for (int i = 0; i < deadWood.size(); ++i) { //find max deadwood values for dead deadwood and for potential sets/runs
			if (hashSetContains(potentialSet,potentialDiscards.get(i)) || hashSetContains(potentialRun,potentialDiscards.get(i))) {
				if (deadWood.get(i) > maxMatchedPotentials) {
					maxMatchedPotentials = deadWood.get(i);
					meldlist.clear();
					meldlist.add(potentialDiscards.get(i));
				}else if (deadWood.get(i) == maxMatchedPotentials) {
					meldlist.add(potentialDiscards.get(i));
				}
			}else {
				if (deadWood.get(i) > maxMatchedDwood) {
					maxMatchedDwood = deadWood.get(i);
					deadlist.clear();
					deadlist.add(potentialDiscards.get(i));
				}else if (deadWood.get(i) == maxMatchedDwood) {
					deadlist.add(potentialDiscards.get(i));
				}
			}
		}
		
		
		//
		//System.out.println("potentials" + deadlist + " " + meldlist);
		//System.out.println(maxDeadDeadWood + " " + maxPotentialMeldDeadWood);
		//
		int maxMatched = (maxMatchedDwood > maxMatchedPotentials)? maxMatchedDwood : maxMatchedPotentials;
		int maxUnmatched = (maxUnmatchedDwood > maxUnmatchedPotentials) ? maxUnmatchedDwood : maxUnmatchedPotentials;
		
		if (maxMatched - maxUnmatched >= 7 || maxUnmatched == -1) {
			/*if (deadlist.isEmpty() && meldlist.isEmpty()) {
				System.out.println(lastDrawnCard);
				System.out.println(potentialDiscards);
				System.out.println(h);
				System.out.println(hand);
			}*/
			 return (maxMatchedDwood >= maxMatchedPotentials && !deadlist.isEmpty())? deadlist.get(0): meldlist.get(0);
		} else {
			if (maxUnmatchedDwood >= 6)
				return dead.get(dIndex);
			else if (maxUnmatchedPotentials >= 6)
				return potentials.get(pIndex);
			else {
				if (dIndex >= 0)
					return dead.get(dIndex);
				else
					return potentials.get(pIndex);
			}
				
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
	private boolean compareCards(Card c1, Card c2) {
		return c1.rank == c2.rank && c1.suit == c2.suit;
	}
	
	private boolean hashSetContains(HashSet<Card> hs, Card c) {
		if (hs.isEmpty())
			return false;
		for (Card hashcard : hs) {
			if (compareCards(hashcard, c))
				return true;
		}
		return false;
	}
	
	
	
	private boolean matches(Card c, ArrayList<Card> h) { //check if a card is a match to a hand
		if (hashSetContains(safeCards(h), c)) //if a card is safe then it cannot match the opponent's hand
			return false;
		for (Card handCard :  h) { 
			if (handCard.rank == c.rank) //check for sets
				return true;
			else if ((handCard.rank == c.rank + 1 || handCard.rank == c.rank -1) && handCard.suit == c.suit) //check for runs
				return true;
		}
		return false;
	}
	
	private HashSet<Card> safeCards(ArrayList<Card> a){
		HashSet<Card> ret = new HashSet<Card>();
		for (Card c: a) {
			int rank = c.rank;
			int suit = c.suit;
			switch (suit) { //cards of the same rank are now safe
			case 0:
				ret.add(new Card(rank,1));
				ret.add(new Card(rank,2));
				ret.add(new Card(rank,3));
				break;
			case 1:
				ret.add(new Card(rank,0));
				ret.add(new Card(rank,2));
				ret.add(new Card(rank,3));
				break;
			case 2:
				ret.add(new Card(rank,0));
				ret.add(new Card(rank,1));
				ret.add(new Card(rank, 3));
				break;
			case 3:
				ret.add(new Card(rank, 0));
				ret.add(new Card(rank, 1));
				ret.add(new Card(rank, 2));
				break;
			}
			//cards of adjacent rank with same suit are now safe
			ret.add(new Card(rank -1,suit));
			ret.add(new Card(rank +1, suit));
		}
		return ret;
	}
	
	private int dWoodFunc(int n) {
		return (int) Math.round(53.88331799*Math.exp(-0.22199779*n) + 4.45358599);
	}
	
	private boolean isPossibleMeld(ArrayList<Card> wantMeld){
		int matchNum = 0;
		for (Card c: wantMeld) {
			if (hashSetContains(seenCards,c))
				++matchNum;
		}
		return matchNum <2;
	}
	
	private boolean isHardMeld(ArrayList<Card> wantMeld) {
		int matchNum = 0;
		for (Card c: wantMeld) {
			if (hashSetContains(seenCards, c))
				++matchNum;
		}
		return matchNum == 1;
	}
}

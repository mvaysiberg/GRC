package ginrummy;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileWriter;
import java.math.*;
import java.util.HashSet;

public class DynamicGinRummyPlayer implements GinRummyPlayer{
	private int playerNum;
	private ArrayList<int[]> gameScores;
	private int[] prevRoundScore;
	private int startingPlayerNum;
	private int randomSetSize;
	private int roundNum;
	private ArrayList<Card> hand;
	private ArrayList<Card> opponentHand;
	private HashSet<Card> opponentPredictions;
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
	private int KNOCK_THRESHOLD_MAX;
	//private int startingDeadWood;
	//private int prevDiff;
	private int gameNum;
	private int X;
	private int Y;
	private int Z;
	private int W;
	private FileWriter deviationWriter;
	private HashMap<Integer, Double[]> netThresholds;
	private boolean usedFirstAlg;
	
	public DynamicGinRummyPlayer() {
		KNOCK_THRESHOLD = 9;
		gameScores = new ArrayList<int[]>();
		KNOCK_THRESHOLD_MAX = 15;
		gameNum = 0;
		X = 0;
		Y = 0;
		Z = 0;
		W = 0;
		netThresholds = new HashMap<Integer, Double[]>();
		usedFirstAlg = false;
	}
	/*	KNOCK_THRESHOLD = knockThreshold;
		gameScores = new ArrayList<int[]>();
		KNOCK_THRESHOLD_MAX = 15;
		gameNum = 0;
		X = 0;
		Y = 0;
		Z = 0;
		W = 0;
		deviationWriter = null;
		netThresholds = new HashMap<Integer, Double[]>();
		usedFirstAlg = false;
	}*/
	
	/*public DynamicGinRummyPlayer(FileWriter f) { //only for collecting deviation data
		KNOCK_THRESHOLD = 9;
		gameScores = new ArrayList<int[]>();
		KNOCK_THRESHOLD_MAX = 15;
		gameNum = 0;
		X = 0;
		Y = 0;
		Z = 0;
		W = 0;
		deviationWriter = f;
		netThresholds = new HashMap<Integer, Double[]>();
		usedFirstAlg = false;
	}*/
	
	@Override
	public void startGame(int playerNum, int startingPlayerNum, Card[] cards) {
		// TODO Auto-generated method stub
		++gameNum;
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
		wantCards = new HashSet<Card>();
		potentialSet = new HashSet<Card>();
		potentialHardSet = new HashSet<Card>();
		potentialRun = new HashSet<Card>();
		potentialHardRun = new HashSet<Card>();
		sets = new HashSet<Card>();
		opponentHand = new ArrayList<Card>();
		updateMeldsDeadWood(hand);
		updateWantCards(hand, wantCards, potentialSet, potentialHardSet, potentialRun, potentialHardRun,sets);
		opponentKnocked = false;
		prevRoundScore = new int[]{0,0};
	}

	@Override
	public boolean willDrawFaceUpCard(Card card) {
		// TODO Auto-generated method stub
		updateMeldsDeadWood(hand);
		updateWantCards(hand, wantCards, potentialSet, potentialHardSet, potentialRun, potentialHardRun, sets);
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
		updateWantCards(tempHand, wantCards, potentialSet, potentialHardSet, potentialRun, potentialHardRun, sets);//this will be undone after report draw anyway
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
		if (drawnCard != null && !hashSetContains(seenCards,drawnCard))
			seenCards.add(drawnCard);
		if (playerNum == this.playerNum) { //Reports what we drew
			lastDrawnCard = drawnCard;
			//drawncard is inserted into hand in the proper sorted position
			insertSorted(drawnCard,hand);
			updateMeldsDeadWood(hand);
			updateWantCards(hand, wantCards, potentialSet, potentialHardSet, potentialRun, potentialHardRun, sets);
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
		updateMeldsDeadWood(hand);
		updateWantCards(hand, wantCards, potentialSet, potentialHardSet, potentialRun, potentialHardRun, sets);
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
		//if(roundNum == 1)
			//startingDeadWood = deadWood;
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
		int diffp0 = scores[0] - prevRoundScore[0];
		int diffp1 = scores[1] - prevRoundScore[1];
		int diff = diffp0 - diffp1;
		diff = (playerNum == 1)? -1*diff: diff;
		
		if (opponentKnocked && diff > 0)
			X += diff;
		else if (!opponentKnocked && diff < 0)
			Y -= diff;
		else if (!opponentKnocked && diff > 0)
			Z += diff;
		else if (opponentKnocked && diff < 0)
			W -= diff;
		if (gameNum % 17 == 0 && (X!= 0 || Y != 0 || Z != 0)) {
			dynamicKnock(X,Y,Z,W,200);
			X = 0;
			Y = 0;
			Z = 0;
			W = 0;
		}
		prevRoundScore = scores;
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
	private void updateMeldsDeadWood(ArrayList<Card> h) {
		sets = new HashSet<Card>();
		bestMelds = GinRummyUtil.cardsToBestMeldSets(h);
		if (bestMelds.isEmpty())
			deadWood = GinRummyUtil.getDeadwoodPoints(h);
		else {
			deadWood = GinRummyUtil.getDeadwoodPoints(bestMelds.get(0), h);
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
	
	private void updateWantCards(ArrayList<Card> h, HashSet<Card> wc, HashSet<Card> ps, HashSet<Card> phs, HashSet<Card> pr, HashSet<Card> phr, HashSet<Card> s) {
		if (wc != null)
			wc.clear();
		if (ps != null)
			ps.clear();
		if (phs != null)
			phs.clear();
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
					wc.add(new Card(cardNum,0));
					possibleS.add(new Card(cardNum,0));
				}if (!suits.contains(1)) {
					wc.add(new Card(cardNum,1));
					possibleS.add(new Card(cardNum,1));
				}if (!suits.contains(2)) {
					wc.add(new Card(cardNum,2));
					possibleS.add(new Card(cardNum,2));
				}if (!suits.contains(3)) {
					wc.add(new Card(cardNum,3));
					possibleS.add(new Card(cardNum,3));
				}
			}
			if (count == 2 && isPossibleMeld(possibleS) && ps != null && phs != null) {
				for (Integer suit: suits) {
					if (!hashSetContains(ps,new Card(cardNum,suit)))
						ps.add(new Card(cardNum,suit));
					if (isHardMeld(possibleS))//create another condition for normal set once discard logic is added
						phs.add(new Card(cardNum,suit));
				}
			}
			i--;
		}
		ArrayList<Card> newHand = new ArrayList<Card>(); //create a new hand without sets so that avoid trying to form runs with current sets
		for (Card c: h) {
			if (s != null && !hashSetContains(s,c))
				newHand.add(c);
		}
		if (pr != null)
			pr.clear();
		if (phr != null)
			phr.clear();
		for (int i = 0; i < newHand.size(); ++i) { //this for loop calculates what cards we want to add to runs or form runs
			int suit = newHand.get(i).suit;
			int startingRank = newHand.get(i).rank;
			int x = i;
			int count = 0;
			ArrayList<Integer> ranks = new ArrayList<Integer>();
			if (hashSetContains(pr,newHand.get(i))) //do not double count the same potential run
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
					wc.add(new Card(ranks.get(0)-1,suit));
					possibleR.add(new Card(ranks.get(0)-1,suit));
				}if (ranks.get(ranks.size() -1) != 12) { //the maximal card in a run is a king
					wc.add(new Card(ranks.get(ranks.size()-1)+1,suit));
					possibleR.add(new Card(ranks.get(ranks.size()-1)+1,suit));
				}if (count == 2 && isPossibleMeld(possibleR) && pr != null && phr != null) {
					for (Integer rank: ranks) {
						pr.add(new Card(rank,suit));
						if (isHardMeld(possibleR)) //create another condition for normal run once discard logic is added
							phr.add(new Card(rank,suit));
					}
					//++numPotentials; //this is a potential run
				}
			}else if (count == 1) { //handle the case when there is a card of the same suit in rank startingRank + 2
				startingRank = newHand.get(i).rank;
				x = i;
				while (x < newHand.size() && newHand.get(x).rank <= startingRank + 2) {
					if (compareCards(newHand.get(x), new Card(startingRank +2,suit))) {
						wc.add(new Card(startingRank + 1, suit));
						if (!hashSetContains(seenCards,new Card(startingRank+1,suit)) && pr != null && phr != null) {
							pr.add(new Card(startingRank,suit)); //can comment out these lines once added new logic in discard algorithm
							pr.add(new Card(startingRank + 2, suit));//
							phr.add(new Card(startingRank,suit));
							phr.add(new Card(startingRank + 2, suit));
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
		updateMeldsDeadWood(hand);
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
		//matching algorithm
		
		opponentPredictions = new HashSet<Card>();
		updateWantCards(opponentHand, opponentPredictions, null, null, null, null, null);
		ArrayList<Card> unseenPredictions = checkSeen(opponentPredictions);
		ArrayList<Card> tempOpponentHand = new ArrayList<Card>(opponentHand);
		for (Card c: unseenPredictions) {
			insertSorted(c, tempOpponentHand);
		}
		HashSet<Card> priorityCards = new HashSet<Card>();
		updateWantCards(tempOpponentHand, priorityCards, null, null, null, null, null);
		ArrayList<Card> priorityMatch = new ArrayList<Card>();
		for (Card c: priorityCards) {
			if (!arrayContains(hand, c))
				insertSorted(c,priorityMatch);
		}
		int priorityIndex = priorityMatch.size() -1;
		
		//System.out.println("hand" + opponentHand.toString());
		//System.out.println("prediction" + tempOpponentHand.toString());
		
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
			if (!matches(potentials.get(pIndex),opponentHand, priorityMatch))
				break;
			--pIndex;
		}
		
		while (dIndex >= 0) { //find the greatest dead deadwood that does not match the opponent's hand
			if (!matches(dead.get(dIndex),opponentHand, priorityMatch))
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
		int maxPriority = priorityIndex >= 0? GinRummyUtil.getDeadwoodPoints(priorityMatch.get(priorityIndex)) : -1;
		if (maxMatched - maxUnmatched >= 5 || maxUnmatched == -1) {
			/*if (deadlist.isEmpty() && meldlist.isEmpty()) {
				System.out.println(lastDrawnCard);
				System.out.println(potentialDiscards);
				System.out.println(h);
				System.out.println(hand);
			}*/
			 return (maxMatchedDwood >= maxMatchedPotentials && !deadlist.isEmpty())? deadlist.get(0): meldlist.get(0);
		}else if (maxPriority - maxUnmatched >= 7 && maxPriority != -1) {
			return priorityMatch.get(priorityIndex);
		}else {
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
		if (hs == null || hs.isEmpty())
			return false;
		for (Card hashcard : hs) {
			if (compareCards(hashcard, c))
				return true;
		}
		return false;
	}
	
	
	
	private boolean matches(Card c, ArrayList<Card> h, ArrayList<Card> p) { //check if a card is a match to a hand
		if (hashSetContains(safeCards(h), c) || arrayContains(p, c)) //if a card is safe then it cannot match the opponent's hand
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
			if (hashSetContains(seenCards,c) || hashSetContains(opponentPredictions, c))
				++matchNum;
		}
		return matchNum <2;
	}
	
	private boolean isHardMeld(ArrayList<Card> wantMeld) {
		int matchNum = 0;
		for (Card c: wantMeld) {
			if (hashSetContains(seenCards, c) || hashSetContains(opponentPredictions,c))
				++matchNum;
		}
		return matchNum == 1;
	}
	
	private double zScore(double x, double mean, double std) {
		return Math.abs(x-mean)/std;
	}
	
	private int knockf1(int diff, boolean weKnocked, double a) {
		if (weKnocked)
			return (int)Math.round(KNOCK_THRESHOLD -a*diff);
		else
			return (int) Math.round(KNOCK_THRESHOLD + a*diff);
	}
	
	private int knockf2(int change) {
		int temp =  KNOCK_THRESHOLD + change;
		if (temp < 0)
			return 0;
		return (temp >= KNOCK_THRESHOLD_MAX)? KNOCK_THRESHOLD_MAX : temp;
	}
	
	/*private int avg(int x, int y) { //arithmetic mean rounded to int
		return (int)Math.round((double)(x + y)/2);
	}*/
	
	private void dynamicKnock(int x, int y, int z, int w, double c) {
		int net = x + z -y - w; //net points
		int max = max(x,y,z); //maximum of x, y, and z
		double dev = deviation(x,y,z); //max - mean
		if (!netThresholds.containsKey((Integer)KNOCK_THRESHOLD)) { //first time seeing this knock threshold
			Double[] d = new Double[] {(double)net,1.0};
			netThresholds.put(KNOCK_THRESHOLD,d);
		}else {
			Double[] d = netThresholds.get(KNOCK_THRESHOLD);
			d[0] = (d[1]*d[0] + net)/(d[1] + 1); //average the net points
			d[1] += 1; //number of times averaged increases by 1
		}
		
		if (deviationWriter != null) {
			try {
				deviationWriter.append(Double.toString(dev));
				deviationWriter.append("\n");
			}catch(Exception e) {
				System.out.println(e);
			}
		}
		
		
		//for (Integer i : netThresholds.keySet()) {
			//Double[] d = netThresholds.get(i);
			//	System.out.print(i + ":[ " + d[0] + ", " + d[1] + " ] ");
		//}
		//System.out.println();
		
		
		//try {
		if ( netThresholds.size() > 1 && netThresholds.get(KNOCK_THRESHOLD)[0] <= getLargestNet(netThresholds) && !usedFirstAlg) { 
			usedFirstAlg = true;
			KNOCK_THRESHOLD = getLargestThreshold(netThresholds);
		}else {
			usedFirstAlg = false;
			if (x == max && dev >= c) {
				KNOCK_THRESHOLD = knockf2(1);
			}else if (y == max) {
				KNOCK_THRESHOLD = knockf2(1);
			}else if (z == max && dev >= c) {
				KNOCK_THRESHOLD = knockf2(-1);
			}
		}
		//}catch(Exception e) {
			//for (Integer i : netThresholds.keySet()) {
			//	Double[] d = netThresholds.get(i);
			//	System.out.print(i + ":[ " + d[0] + ", " + d[1] + " ] ");
			//}
			//System.out.println();
		//}
	}
	
	public int getKnockThreshold() {
		return KNOCK_THRESHOLD;
	}
	
	private double aValue(double curThreshold) {
		return Math.abs(0.5/curThreshold);
	}
	
	private int max(double x, double y, double z) {
		return (int)Math.max(Math.max(x, y), z);
	}
	
	private double deviation(double x, double y, double z) {
		return max(x,y,z) - (x + y + z)/3;
	}
	
	private int getLargestThreshold(HashMap<Integer,Double[]>h) {
		double maxPoints = Double.NEGATIVE_INFINITY; //temporary value that should never be returned
		int maxThreshold = Integer.MIN_VALUE;//temporary value that should never be returned
		
		for (Integer threshold: h.keySet()) {
			double points = h.get(threshold)[0];
			if (points > maxPoints) {
				maxPoints = points;
				maxThreshold = threshold;
			}
		}
		//System.out.println(maxThreshold + " " + Integer.MIN_VALUE + " " + maxPoints + " " + Double.MIN_VALUE);
		return maxThreshold;
	}
	
	private double getLargestNet(HashMap<Integer,Double[]>h) {
		//System.out.println(h.toString());
		return h.get(getLargestThreshold(h))[0];
	}
	
	private ArrayList<Card> checkSeen(HashSet<Card> h){
		ArrayList<Card> unseen = new ArrayList<Card>();
		for (Card c: h) {
			if (!hashSetContains(seenCards, c)) {
				insertSorted(c, unseen);
			}
		}
		return unseen;
	}
	
	private boolean arrayContains(ArrayList<Card> a, Card c) {
		for (Card check: a) {
			if (compareCards(check, c))
				return true;
		}
		return false;
	}
	
	public HashMap<Integer, Double[]> getThresholds(){
		return netThresholds;
	}
}

package modules.transitionNetwork;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * A class that implements a transition network as a 2-dimensional array over
 * states and labels.
 */
public class TransitionNetworkArray {

	private final int[][] network;

	private final String[] transitions;

	private final Map<String, Integer> transitionIndices;

	private int statesAmount;
	private int transitionsAmount;

	private int maxStates;
	private int maxTransitions;

	private static final int FINAL_STATE = Integer.MAX_VALUE;

	private static final int INITIAL_STATE = 0;

	private static final int INVALID_STATE = -1;

	public TransitionNetworkArray(int maxStates, int maxTransitions) {
		if (maxStates < 1 || maxTransitions < 1) {
			throw new IllegalArgumentException("Dimensions of states or labels must be > 0");
		}

		// intialize the main network's array to the invalid state
		this.network = new int[maxStates][maxTransitions];
		for (int i = 0; i < maxStates; i++) {
			Arrays.fill(network[i], INVALID_STATE);
		}
		this.transitions = new String[maxTransitions];

		// keep max values for bound's checks
		this.maxStates = maxStates;
		this.maxTransitions = maxTransitions;

		// keep track of how many state's and transitions have been registered
		this.statesAmount = 0;
		this.transitionsAmount = 0;

		// keep a register of transition strings to their index
		this.transitionIndices = new TreeMap<>();
	}

	public int addTransitionToNewState(int fromState, String transition) {
		if (!stateExists(fromState)) {
			throw new IllegalArgumentException("State " + fromState + " does not exist.");
		}

		int transitionIdx = getOrAddTransitionIndex(transition);
		int toState = incrementStatesAmount();
		network[fromState][transitionIdx] = toState;

		return toState;
	}

	public int addTransitionToFinalState(int fromState, String transition) {
		if (!stateExists(fromState)) {
			throw new IllegalArgumentException("State " + fromState + " does not exist.");
		}

		int transitionIdx = getOrAddTransitionIndex(transition);
		network[fromState][transitionIdx] = FINAL_STATE;

		return FINAL_STATE;
	}

	public void addPath(String[] path) {
		int state = INITIAL_STATE;
		Integer transitionIdx;
		int idx = 0;

		// follow the path as long as possible
		for (; idx < path.length; idx++) {
			transitionIdx = transitionIndices.get(path[idx]);

			if (transitionIdx == null || network[state][transitionIdx] == INVALID_STATE) {
				break;
			} else {
				state = network[state][transitionIdx];
				idx += 1;
			}
		}

		// for the rest of the path (minus the last element) add new transitions
		for (; idx < path.length - 1; idx++) {
			state = addTransitionToNewState(state, path[idx]);
		}

		// add a transition to the final state for the last path element
		addTransitionToFinalState(state, path[path.length - 1]);
	}

	private int getOrAddTransitionIndex(String transition) {
		Integer idx = transitionIndices.get(transition);
		if (idx == null) {
			idx = incrementTransitionsAmount() - 1;
			transitions[idx] = transition;
			transitionIndices.put(transition, idx);
		}
		return idx;
	}

	public boolean stateExists(int state) {
		return (state == FINAL_STATE || (state >= 0 && state <= statesAmount));
	}

	// safely increment the amount of states and return the new value
	private int incrementStatesAmount() {
		if (statesAmount + 1 > maxStates) {
			throw new IllegalStateException("State limit reached.");
		}
		statesAmount += 1;
		return statesAmount;
	}

	// safely increment the amount of transitions and return the new value
	private int incrementTransitionsAmount() {
		if (transitionsAmount + 1 > maxTransitions) {
			throw new IllegalStateException("Transition limit reached.");
		}
		transitionsAmount += 1;
		return transitionsAmount;
	}

	public double stateDistance(int stateOne, int stateTwo) {
		if (!stateExists(stateOne))
			throw new IllegalArgumentException("State does not exist: " + stateOne);
		if (!stateExists(stateTwo))
			throw new IllegalArgumentException("State does not exist: " + stateTwo);

		return new StateDistanceComparator(stateOne, stateTwo).compare();
	}

	public int countSubStates(int state) {
		if (!stateExists(state)) {
			throw new IllegalArgumentException("State does not exist: " + state);
		}
		int result = 0;
		
		for (int i = 0; i < transitionsAmount; i++) {
			switch (network[state][i]) {
			case INVALID_STATE:
				break;
			case FINAL_STATE:
				result += 1;
				break;
			default:
				result += countSubStates(network[state][i]);
				break;
			}
		}

		return result;
	}

	public String print() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < transitionsAmount; i++) {
			sb.append(transitions[i]);
			sb.append(",");
		}
		sb.setLength(sb.length() - 1);
		sb.append('\n');

		for (int i = 0; i <= statesAmount; i++) {
			sb.append('S');
			sb.append(i);
			sb.append(": ");
			for (int j = 0; j < transitionsAmount; j++) {
				if (network[i][j] != INVALID_STATE) {
					sb.append(transitions[j]);
					sb.append(" => S");
					if (network[i][j] == FINAL_STATE) {
						sb.append('F');
					} else {
						sb.append(network[i][j]);
					}
					sb.append(", ");
				}
			}
			sb.setLength(sb.length() - 2);
			sb.append('\n');
		}

		return sb.toString();
	}

	public int getStatesAmount() {
		return statesAmount;
	}

	public int getTransitionsAmount() {
		return transitionsAmount;
	}

	public int getMaxStates() {
		return maxStates;
	}

	public int getMaxTransitions() {
		return maxTransitions;
	}

	public int getFinalState() {
		return FINAL_STATE;
	}

	public int getInitialState() {
		return INITIAL_STATE;
	}

	public int getInvalidState() {
		return INVALID_STATE;
	}

	private class StateDistanceComparator {

		Double distance = null;

		int match = 0;
		int noMatch = 0;

		int one;
		int two;

		StateDistanceComparator(int stateOne, int stateTwo) {
			this.one = stateOne;
			this.two = stateTwo;
		}

		double compare() {
			if (distance != null) {
				return distance;
			}

			helpCompare(one, two);

			distance = 1 - ((double) match / (match + noMatch));

			return distance;
		}

		void helpCompare(int stateOne, int stateTwo) {

			int transOne;
			int transTwo;

			for (int i = 0; i < transitionsAmount; i++) {
				transOne = network[stateOne][i];
				transTwo = network[stateTwo][i];

				if (transOne != transTwo) {
					// if the transitions do not match simply count their
					// children and add them to the count of non matching
					if (transOne != INVALID_STATE) {
						noMatch += 1;
						noMatch += countSubStates(transOne);
					}
					if (transTwo != INVALID_STATE) {
						noMatch += 1;
						noMatch += countSubStates(transTwo);
					}
				} else {
					// in case of a match, check what the match consist in.
					// Invalid States do not count towards anything. Final
					// states count and terminate the search. Other matches
					// count and let the search recurse.
					switch (transOne) {
					case INVALID_STATE:
						break;
					case FINAL_STATE:
						match += 2;
						break;
					default:
						match += 2;
						helpCompare(transOne, transTwo);
						break;
					}
				}
			}
		}
	}

}

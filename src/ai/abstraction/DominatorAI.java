package ai.abstraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PartiallyObservableGameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class DominatorAI extends AbstractionLayerAI {
	UnitTypeTable a_utt;
	UnitType workerType;
	UnitType baseType;
	UnitType barracksType;
	UnitType lightType;
	UnitType rangeType;
	UnitType heavyType;
	public static final String LIGHT_TYPE = "Light";
	public static final String HEAVY_TYPE = "Heavy";
	public static final String RANGE_TYPE = "Ranged";
	public static final String WORKER_TYPE = "Worker";
	public static final String BASE_TYPE = "Base";
	public static final String BARRACKS_TYPE = "Barracks";
	int noOfEnemyLightUnits;
	int noOfEnemyRangeUnits;
	int noOfEnemyHeavyUnits;
	int noOfEnemyWorkerUnits;
	int noOfEnemyBarracks;
	int noOfEnemyBases;
	int noOfPlayerLightUnits;
	int noOfPlayerRangeUnits;
	int noOfPlayerHeavyUnits;
	int defaultCoalitionValue = 5;
	int noOfObservedEnemyLightUnits;
	int noOfObservedEnemyRangeUnits;
	int noOfObservedEnemyHeavyUnits;
	int noOfObservedEnemyWorkerUnits;
	Random random = new Random(4); // use seed
	List<Unit> resourceLocations = new ArrayList<Unit>();
	List<Unit> baseLocations = new ArrayList<Unit>();
	boolean observedCell[][];
	Position lastSeenEnemyPosition = new Position();
	GameState preGameState;
	HashMap<String, Unit> positionForWorker = new HashMap<String, Unit>();
	double phc = 0.6;
	double plc = 0.4;
	double pd = 0.5;
	double pe1 = 0.3;
	double pe2 = 0.3;
	double pe3 = 0.3;

	public DominatorAI(UnitTypeTable a_utt) {
		this(a_utt, new AStarPathFinding());
	}

	public DominatorAI(UnitTypeTable a_utt, PathFinding a_pf) {
		super(a_pf);
		reset(a_utt);
	}

	public void reset() {
		super.reset();
	}

	public void reset(UnitTypeTable a_utt) {
		this.a_utt = a_utt;
		workerType = a_utt.getUnitType(WORKER_TYPE);
		baseType = a_utt.getUnitType(BASE_TYPE);
		barracksType = a_utt.getUnitType(BARRACKS_TYPE);
		lightType = a_utt.getUnitType(LIGHT_TYPE);
		rangeType = a_utt.getUnitType(RANGE_TYPE);
		heavyType = a_utt.getUnitType(HEAVY_TYPE);
	}

	public AI clone() {
		return new DominatorAI(a_utt, pf);
	}

	/*
	 * @Override public void preGameAnalysis(GameState gs, long milliseconds) throws
	 * Exception { // TODO Auto-generated method stub // super.preGameAnalysis(gs,
	 * milliseconds); preGameState = gs.clone(); }
	 */

	class Position {
		int x;
		int y;

		Position(int x, int y) {
			this.x = x;
			this.y = y;
		}

		Position() {

		}

		int getX() {
			return this.x;
		}

		void setY(int y) {
			this.y = y;
		}

		void setX(int x) {
			this.x = x;
		}

		int getY() {
			return this.y;
		}

		public String toString() {
			return "(" + this.x + "," + this.y + ")";
		}
	}

	/*
	 * This is the main function of the AI. It is called at each game cycle with the
	 * most up to date game state and returns which actions the AI wants to execute
	 * in this cycle. The input parameters are: - player: the player that the AI
	 * controls (0 or 1) - gs: the current game state This method returns the
	 * actions to be sent to each of the units in the gamestate controlled by the
	 * player, packaged as a PlayerAction.
	 */
	public PlayerAction getAction(int player, GameState gs) {
		// TODO Auto-generated method stub
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);

		noOfEnemyLightUnits = Math.max(countUnits(p, pgs, LIGHT_TYPE, true), noOfObservedEnemyLightUnits);
		noOfEnemyHeavyUnits = Math.max(countUnits(p, pgs, HEAVY_TYPE, true), noOfObservedEnemyHeavyUnits);
		noOfEnemyRangeUnits = Math.max(countUnits(p, pgs, RANGE_TYPE, true), noOfObservedEnemyRangeUnits);
		noOfEnemyWorkerUnits = Math.max(countUnits(p, pgs, WORKER_TYPE, true), noOfObservedEnemyWorkerUnits);

		noOfPlayerLightUnits = countUnits(p, pgs, LIGHT_TYPE, false);
		noOfPlayerHeavyUnits = countUnits(p, pgs, HEAVY_TYPE, false);
		noOfPlayerRangeUnits = countUnits(p, pgs, RANGE_TYPE, false);

		// base behaviour
		for (Unit u : pgs.getUnits()) {
			if (u.getType() == baseType && u.getPlayer() == player && gs.getActionAssignment(u) == null) {
				baseBehavior(u, p, pgs);
			}
		}

		// behavior of barracks:
		for (Unit u : pgs.getUnits()) {
			if (u.getType() == barracksType && u.getPlayer() == player && gs.getActionAssignment(u) == null) {
				barracksBehavior(u, p, pgs);
			}
		}

		// behavior of workers:
		List<Unit> workers = new LinkedList<>();
		for (Unit u : pgs.getUnits()) {
			if (u.getType().canHarvest && u.getPlayer() == player) {
				workers.add(u);
			}
		}

		workersBehavior(workers, p, gs);

		// behavior of melee units:
		for (Unit u : pgs.getUnits()) {
			if (u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == player
					&& gs.getActionAssignment(u) == null) {
				meleeUnitBehavior(u, p, gs);
			}
		}

		// System.out.println(countObservedCell(pgs));
		return translateActions(player, gs);
	}

	void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
		int nworkers = 0;
		int minWorkers = 0;
		int nbases = 0;

		for (Unit u2 : pgs.getUnits()) {
			if (u2.getType() == baseType && u2.getPlayer() == p.getID()) {
				nbases++;
			}
		}
		minWorkers = nbases * 2 + 1;
		for (Unit u2 : pgs.getUnits()) {
			if (u2.getType() == workerType) {
				if (u2.getPlayer() != p.getID()) {
					noOfEnemyWorkerUnits++;
				} else {
					nworkers++;
				}
			}
		}

		while (nworkers < minWorkers) {
			if (isResourceEnoughToTrain(p, workerType)) {
				train(u, workerType);
			}
			nworkers++;
		}
	}

	void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
		String type = addToCoalitions(p, pgs);
		switch (type) {
		case LIGHT_TYPE:
			if (isResourceEnoughToTrain(p, lightType)) {
				train(u, lightType);
			}
			break;
		case RANGE_TYPE:
			if (isResourceEnoughToTrain(p, rangeType)) {
				train(u, rangeType);
			}
			break;
		case HEAVY_TYPE:
			if (isResourceEnoughToTrain(p, heavyType)) {
				train(u, heavyType);
			}
		}
	}

	void meleeUnitBehavior(Unit u, Player p, GameState gs) {
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Unit closestEnemy = findClosestEnemy(u, pgs, p);

		if (closestEnemy != null) {
			lastSeenEnemyPosition.setX(closestEnemy.getX());
			lastSeenEnemyPosition.setY(closestEnemy.getY());
			attack(u, closestEnemy);
		} else if (gs instanceof PartiallyObservableGameState) {
			if (lastSeenEnemyPosition.getX() != u.getX() && lastSeenEnemyPosition.getY() != u.getY()) {
				move(u, lastSeenEnemyPosition.getX(), lastSeenEnemyPosition.getY());
			} else {
				PartiallyObservableGameState pogs = (PartiallyObservableGameState) gs;
				// there are no enemies, so we need to explore (find the nearest non-observable
				// place):
				int closest_x = 0;
				int closest_y = 0;
				int closestDistance = -1;
				for (int i = 0; i < pgs.getHeight(); i++) {
					for (int j = 0; j < pgs.getWidth(); j++) {
						if (!pogs.observable(j, i)) {
							int d = (u.getX() - j) * (u.getX() - j) + (u.getY() - i) * (u.getY() - i);
							if (closestDistance == -1 || d < closestDistance) {
								closest_x = j;
								closest_y = i;
								closestDistance = d;
							}
						}
					}
				}
				if (closestDistance != -1) {
					move(u, closest_x, closest_y);
				}
			}
		}
	}

	void workersBehavior(List<Unit> workers, Player p, GameState gs) {
		PhysicalGameState pgs = gs.getPhysicalGameState();
		int nbases = 0;
		int nbarracks = 0;
		int harvestWorkers = 0;
		boolean buildBaseInProgress = false;
		boolean buildBarrackInProgress = false;
		int resourcesUsed = 0;
		// int bufferResources = 1;
		int shortestDistanceToBase = 5;
		Position workerPosition = null;

		if (workers.isEmpty()) {
			return;
		}

		for (Unit u2 : pgs.getUnits()) {
			if (u2.getType() == baseType && u2.getPlayer() == p.getID()) {
				nbases++;
				if (!baseLocations.contains(u2)) {
					baseLocations.add(u2);
				}
			}
		}

		for (Unit u2 : pgs.getUnits()) {
			if (u2.getType() == barracksType && u2.getPlayer() == p.getID()) {
				nbarracks++;
			}
		}

		updateResourcePositions(gs);

		harvestWorkers = nbases * 2;

		boolean hasScoutWorkers = workers.size() > harvestWorkers;

		List<Unit> freeWorkers = hasScoutWorkers ? new LinkedList<>(workers.subList(0, harvestWorkers))
				: new LinkedList<>(workers);

		List<Unit> scouts = new ArrayList<Unit>();

		if (hasScoutWorkers) {
			scouts = new ArrayList<>(workers.subList(harvestWorkers, workers.size()));
		}

		List<Integer> reservedPositions = new LinkedList<>();
		if (nbases == 0 && !freeWorkers.isEmpty() && !buildBaseInProgress) {
			// build a base:
			if (p.getResources() >= baseType.cost + resourcesUsed) {
				Unit u = freeWorkers.remove(0);
				if (buildBase(p, pgs, u, reservedPositions, null)) {
					resourcesUsed += baseType.cost;
				}
			}
		}

		if ((nbarracks == 0 || (p.getResources() >= 3 * barracksType.cost)) && !freeWorkers.isEmpty()) {
			// build a barracks
			if (p.getResources() >= (barracksType.cost + resourcesUsed)) {
				Unit u = freeWorkers.remove(0);
				buildBarrackInProgress = buildBarracks(p, pgs, resourcesUsed, u, reservedPositions, baseLocations);
				if (buildBarrackInProgress) {
					resourcesUsed += barracksType.cost;
				}
			}
		}

		for (Unit u : freeWorkers) {
			Unit closestBase = findClosestBase(u, pgs, p);
			Unit closestResource = findClosestResource(u, pgs, p);

			if (closestResource != null && !resourceLocations.contains(closestResource)) {
				resourceLocations.add(closestResource);
				// System.out.println("resource added");
			}

			if (closestResource == null && (gs instanceof PartiallyObservableGameState)) {
				if (!resourceLocations.isEmpty()) {
					Unit resource = resourceLocations.get(0);
					if (resource != null) {
						move(u, resource.getX(), resource.getY());
					}
				} else {
					if (isUnitStationary(u)) {
						workerPosition = findResource(u, pgs, gs);
						move(u, workerPosition.getX(), workerPosition.getY());
					}
					continue;
				}
			}

			if (closestResource != null && closestBase != null && manhattanDistance(closestResource.getX(),
					closestResource.getY(), closestBase.getX(), closestBase.getY()) > shortestDistanceToBase
					&& !buildBaseInProgress) {
				if (p.getResources() >= baseType.cost + resourcesUsed) {
					if (buildBase(p, pgs, u, reservedPositions, closestResource)) {
						resourcesUsed += baseType.cost;
						buildBaseInProgress = true;
						continue;
					}
				}
			}

			if (nbases != 0) {
				collectResourceAndHarvest(u, closestBase, closestResource);
			}

		}

		for (Unit u : scouts) {
			scoutEnemy(u, p, pgs, gs);
		}

	}

	void updateResourcePositions(GameState gs) {

		if (resourceLocations == null) {
			return;
		}

		PartiallyObservableGameState pogs = null;
		boolean isPOGameState = false;
		if (gs instanceof PartiallyObservableGameState) {
			isPOGameState = true;
			pogs = (PartiallyObservableGameState) gs;
		}

		if (isPOGameState) {
			Iterator<Unit> resourceIterator = resourceLocations.listIterator();
			while (resourceIterator.hasNext()) {
				Unit resource = resourceIterator.next();
				if (pogs.observable(resource.getX(), resource.getY()) && gs.getUnit(resource.getID()) == null) {
					resourceIterator.remove();
					// System.out.println("resource removed");
				}
			}
		}
	}

	boolean isUnitStationary(Unit unit) {
		AbstractAction action = getAbstractAction(unit);
		return (action == null || !(action instanceof Move));
	}

	Unit findClosestResource(Unit u, PhysicalGameState pgs, Player p) {
		Unit closestResource = null;
		int closestDistance = 0;
		for (Unit u2 : pgs.getUnits()) {
			if (u2.getType().isResource) {
				int d = manhattanDistance(u2.getX(), u2.getY(), u.getX(), u.getY());
				if (closestResource == null || d < closestDistance) {
					closestResource = u2;
					closestDistance = d;
				}
			}
		}
		return closestResource;
	}

	Unit findClosestBase(Unit u, PhysicalGameState pgs, Player p) {
		Unit closestBase = null;
		int closestDistance = 0;
		for (Unit u2 : pgs.getUnits()) {
			if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
				int d = manhattanDistance(u2.getX(), u2.getY(), u.getX(), u.getY());
				if (closestBase == null || d < closestDistance) {
					closestBase = u2;
					closestDistance = d;
				}
			}
		}
		return closestBase;
	}

	Unit findClosestEnemy(Unit u, PhysicalGameState pgs, Player p) {
		Unit closestEnemy = null;
		int closestDistance = 0;
		for (Unit u2 : pgs.getUnits()) {
			if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
				int d = manhattanDistance(u2.getX(), u2.getY(), u.getX(), u.getY());
				if (closestEnemy == null || d < closestDistance) {
					closestEnemy = u2;
					closestDistance = d;
				}
			}
		}
		return closestEnemy;
	}

	String addToCoalitions(Player p, PhysicalGameState pgs) {
		return findMaxCoalition();
	}

	String findMaxCoalition() {
		int heavyValue = getCoalitionValue(noOfPlayerRangeUnits, noOfPlayerHeavyUnits, noOfEnemyHeavyUnits);
		int lightValue = getCoalitionValue(noOfPlayerHeavyUnits, noOfPlayerLightUnits, noOfEnemyLightUnits);
		int rangeValue = getCoalitionValue(noOfPlayerLightUnits, noOfPlayerRangeUnits, noOfEnemyRangeUnits);

		double rValue = random.nextDouble();
		if (heavyValue < lightValue && heavyValue < rangeValue) {
			if (rValue < phc) {
				return RANGE_TYPE;
			}
			return HEAVY_TYPE;
		} else if (heavyValue > lightValue && lightValue < rangeValue) {
			if (rValue < plc) {
				return HEAVY_TYPE;
			}
			return LIGHT_TYPE;
		} else if (heavyValue == lightValue && heavyValue == rangeValue) {
			if (rValue < pe1 && rValue > pe2) {
				return RANGE_TYPE;
			} else if (rValue < pe3) {
				return HEAVY_TYPE;
			} else {
				return LIGHT_TYPE;
			}
		} else {
			if (rValue < pd) {
				return LIGHT_TYPE;
			}
			return RANGE_TYPE;
		}
	}

	int getCoalitionValue(int effectiveUnit, int sameUnit, int enemyUnit) {
		if (enemyUnit == 0) {
			return defaultCoalitionValue;
		}

		int value = effectiveUnit + sameUnit - enemyUnit;
		return value;
	}

	public int countUnits(Player p, PhysicalGameState pgs, String type, boolean isEnemyUnits) {
		int noOfUnits = 0;
		if (isEnemyUnits) {
			for (Unit u2 : pgs.getUnits()) {
				if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID() && u2.getType().name.equals(type)) {
					noOfUnits++;
				}
			}
		} else {
			for (Unit u2 : pgs.getUnits()) {
				if (u2.getPlayer() >= 0 && u2.getPlayer() == p.getID() && u2.getType().name.equals(type)) {
					noOfUnits++;
				}
			}
		}
		return noOfUnits;
	}

	boolean isResourceEnoughToTrain(Player p, UnitType unitType) {
		if (p.getResources() > (unitType.cost)) {
			return true;
		}
		return false;
	}

	void scoutEnemy(Unit unit, Player p, PhysicalGameState pgs, GameState gs) {
		noOfObservedEnemyLightUnits = Math.max(countUnits(p, pgs, LIGHT_TYPE, true), noOfObservedEnemyLightUnits);
		noOfObservedEnemyHeavyUnits = Math.max(countUnits(p, pgs, HEAVY_TYPE, true), noOfObservedEnemyHeavyUnits);
		noOfObservedEnemyRangeUnits = Math.max(countUnits(p, pgs, RANGE_TYPE, true), noOfObservedEnemyRangeUnits);
		noOfObservedEnemyWorkerUnits = Math.max(countUnits(p, pgs, WORKER_TYPE, true), noOfObservedEnemyWorkerUnits);
		Unit closestEnemy = findClosestEnemy(unit, pgs, p);
		if (closestEnemy != null) {
			lastSeenEnemyPosition.setX(closestEnemy.getX());
			lastSeenEnemyPosition.setY(closestEnemy.getY());
			attack(unit, closestEnemy);
		} else {
			if (isUnitStationary(unit)) {
				Position position = findScoutingPositions(unit, pgs);
				move(unit, position.getX(), position.getY());
			}
		}
	}

	Position findScoutingPositions(Unit unit, PhysicalGameState pgs) {
		Position position = new Position();
		if (!baseLocations.isEmpty()) {
			Unit base = baseLocations.get(0);
			double rValue = random.nextDouble();
			if (rValue < 0.25) {
				position.setX(pgs.getWidth() - base.getX() - 1);
				position.setY(base.getY());
			} else if (rValue > 0.5 && rValue < 0.75) {
				position.setX(base.getX());
				position.setY(pgs.getHeight() - base.getY() - 1);
			} else {
				position.setX(pgs.getWidth() - base.getX() - 1);
				position.setY(pgs.getHeight() - base.getY() - 1);
			}
		}
		return position;
	}

	Position findFreeCellInSquare(PhysicalGameState pgs, int x, int y) {
		int cellSize = 2;
		Position position = new Position();
		int xStart = x - cellSize < 0 ? 0 : x - cellSize;
		int yStart = y - cellSize < 0 ? 0 : y - cellSize;

		int xlimit = x + cellSize > pgs.getWidth() ? pgs.getWidth() - 1 : x + cellSize;
		int ylimit = y + cellSize > pgs.getHeight() ? pgs.getHeight() - 1 : y + cellSize;

		boolean free[][] = pgs.getAllFree();

		for (int i = xStart + 1; i < xlimit; i++) {
			for (int j = ylimit; j < yStart + 1; j--) {
				if (free[i][j]) {
					if (isPositionOnMap(i - 1, j, pgs) && free[i - 1][j] && isPositionOnMap(i, j - 1, pgs)
							&& free[i][j - 1]) {
						position.setX(i);
						position.setY(j);
						return position;
					}
				}
			}
		}
		return position;
	}

	Position findResource(Unit worker, PhysicalGameState pgs, GameState gs) {
		// updateObservedCells(pgs, gs);
		Position newPosition = new Position(worker.getX(), worker.getY());

		boolean isLocatedInFirstHalf = isLocatedInFirstHalf(worker.getX(), pgs);

		if (positionForWorker.isEmpty()) {
			positionForWorker.put("firstHalf", worker);
		}

		if (isLocatedInFirstHalf) {
			if (positionForWorker.get("firstHalf") != null && positionForWorker.get("firstHalf") == worker) {
				newPosition = takeRandomStep(worker, pgs, 0, pgs.getWidth() / 2, 0, pgs.getHeight() / 2);
			} else {
				newPosition = takeRandomStep(worker, pgs, 0, pgs.getWidth() / 2, pgs.getHeight() / 2, pgs.getHeight());
			}
		} else {
			if (positionForWorker.get("firstHalf") != null && positionForWorker.get("firstHalf") == worker) {
				newPosition = takeRandomStep(worker, pgs, pgs.getWidth() / 2, pgs.getWidth(), 0, pgs.getHeight() / 2);
			} else {
				newPosition = takeRandomStep(worker, pgs, pgs.getWidth() / 2, pgs.getWidth(), pgs.getHeight() / 2,
						pgs.getHeight());
			}
		}

		return newPosition;
	}

	Position takeRandomStep(Unit u, PhysicalGameState pgs, int xMin, int xMax, int yMin, int yMax) {
		Position position = new Position();
		position.setX(u.getX());
		position.setY(u.getY());

		int x = random.nextInt(xMax - xMin) + xMin;
		int y = random.nextInt(yMax - yMin) + yMin;

		if (isPositionOnMap(x, y, pgs)) {
			position.setX(x);
			position.setY(y);
		}
		return position;
	}

	/*
	 * int countObservedCell(PhysicalGameState pgs) { int count = 0; if
	 * (observedCell != null) { for (int j = 0; j < pgs.getHeight(); j++) { for (int
	 * i = 0; i < pgs.getWidth(); i++) { if (observedCell[i][j]) { count++; } } } }
	 * return count; }
	 */

	/*
	 * Position findShortestUnobservableDistance(Unit u, PhysicalGameState pgs,
	 * GameState gs) { Position position = new Position(); int closestDistance =
	 * Integer.MAX_VALUE; PartiallyObservableGameState pogs =
	 * (PartiallyObservableGameState) gs; updateObservedCells(pgs, gs); for (int j =
	 * 0; j < pgs.getHeight(); j++) { for (int i = 0; i < pgs.getWidth(); i++) { if
	 * (!observedCell[i][j]) {
	 * 
	 * int distance = manhattanDistance(u.getX(), u.getY(), i, j); if (distance <
	 * closestDistance) { closestDistance = distance;
	 * 
	 * position.setX(i); position.setY(j); break; // } } } }
	 * 
	 * return position; }
	 */

	boolean isLocatedInFirstHalf(int x, PhysicalGameState pgs) {
		if (x < pgs.getWidth() / 2 && x >= 0) {
			return true;
		}
		return false;

	}

	/*
	 * void updateObservedCells(PhysicalGameState pgs, GameState gs) { if
	 * (observedCell == null) { observedCell = new
	 * boolean[pgs.getWidth()][pgs.getHeight()]; }
	 * 
	 * PartiallyObservableGameState pogs = (PartiallyObservableGameState) gs; for
	 * (int j = 0; j < pgs.getHeight(); j++) { for (int i = 0; i < pgs.getWidth();
	 * i++) { if (!observedCell[i][j] && pogs.observable(i, j)) { observedCell[i][j]
	 * = true; } } }
	 * 
	 * }
	 */

	boolean buildBase(Player p, PhysicalGameState pgs, Unit worker, List<Integer> reservedPositions,
			Unit closestResource) {
		boolean isSuccess = false;
		Position position = closestResource != null
				? findFreeCellInSquare(pgs, closestResource.getX(), closestResource.getY())
				: findFreeCellInSquare(pgs, worker.getX(), worker.getY());

		if (position.getX() == 0 && position.getY() == 0) {
			isSuccess = buildIfNotAlreadyBuilding(worker, baseType, worker.getX(), worker.getY(), reservedPositions, p,
					pgs);
		} else {
			buildIfNotAlreadyBuilding(worker, baseType, position.getX(), position.getY(), reservedPositions, p, pgs);
			isSuccess = true;
		}
		return isSuccess;
	}

	boolean buildBarracks(Player p, PhysicalGameState pgs, int resourcesUsed, Unit worker,
			List<Integer> reservedPositions, List<Unit> baseLocations) {
		boolean isSuccess = false;
		isSuccess = buildIfNotAlreadyBuilding(worker, barracksType, worker.getX(), worker.getY(), reservedPositions, p,
				pgs);
		return isSuccess;
	}

	void collectResourceAndHarvest(Unit worker, Unit closestBase, Unit closestResource) {
		if (worker.getResources() > 0) {
			if (closestBase != null) {
				AbstractAction aa = getAbstractAction(worker);
				if (aa instanceof Harvest) {
					Harvest h_aa = (Harvest) aa;
					if (h_aa.getBase() != closestBase)
						harvest(worker, null, closestBase);
				} else {
					harvest(worker, null, closestBase);
				}
			}
		} else {
			if (closestResource != null && closestBase != null) {
				AbstractAction aa = getAbstractAction(worker);
				if (aa instanceof Harvest) {
					Harvest h_aa = (Harvest) aa;
					if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase)
						harvest(worker, closestResource, closestBase);
				} else {
					harvest(worker, closestResource, closestBase);
				}

			}
		}
	}

	/*
	 * Position findPositionForBarracks(List<Position> baseUnits, PhysicalGameState
	 * pgs) { Position position = null;
	 * 
	 * if (baseUnits.isEmpty()) { return position; }
	 * 
	 * Position p = baseUnits.get(0); int x = p.getX(); int y = p.getY(); position =
	 * findFreeCellInSquare(pgs, x, y); return position; }
	 */

	int manhattanDistance(int x1, int y1, int x2, int y2) {
		return Math.abs(x1 - x2) + Math.abs(y2 - y1);
	}

	boolean isPositionOnMap(int x, int y, PhysicalGameState pgs) {
		if (x > pgs.getWidth() - 1 || x < 0 || y > pgs.getHeight() - 1 || y < 0) {
			return false;
		}
		return true;
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		List<ParameterSpecification> parameters = new ArrayList<>();

		parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

		return parameters;
	}

}
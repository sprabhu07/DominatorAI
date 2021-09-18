package tests;

import java.io.File;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import ai.abstraction.DominatorAI;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ContinuingAI;
import ai.core.InterruptibleAI;
import ai.core.PseudoContinuingAI;
import ai.mcts.believestatemcts.BS3_NaiveMCTS;
import ai.poadaptive.POAdaptive;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class Results2 implements Runnable{
	boolean CONTINUING = true;
	int TIME = 100;
	int MAX_ACTIONS = 100;
	int MAX_PLAYOUTS = -1;
	int PLAYOUT_TIME = 100;
	int MAX_DEPTH = 10;
	int RANDOMIZED_AB_REPEATS = 10;

	List<AI> bots = new LinkedList<>();
	UnitTypeTable utt = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED);
	
	PrintStream out = null;
	

	// Separate the matchs by map:
	List<PhysicalGameState> maps = new LinkedList<>();
	
	void init() throws Exception{
		bots.add(new POAdaptive(utt, "src/ai/poadaptive/distributions.xml", "src/ai/poadaptive/distribution_woutb.xml", "src/ai/poadaptive/"));
		bots.add(new DominatorAI(utt));
		PhysicalGameState pgs = PhysicalGameState.load("maps/barricades24x24.xml",utt);
		GameState gs = new GameState(pgs, utt);

		if (CONTINUING) {
			// Find out which of the bots can be used in "continuing" mode:
			List<AI> bots2 = new LinkedList<>();
			for (AI bot : bots) {
				if (bot instanceof BS3_NaiveMCTS || bot instanceof POAdaptive) {
					bot.preGameAnalysis(gs, 100);
				}
				if (bot instanceof AIWithComputationBudget) {
					if (bot instanceof InterruptibleAI) {
						bots2.add(new ContinuingAI(bot));
					} else {
						bots2.add(new PseudoContinuingAI((AIWithComputationBudget) bot));
					}
				} else {
					bots2.add(bot);
				}
			}
			bots = bots2;
		}


		out = new PrintStream(new File("DominatorAIvsMicrophantom2.txt"));
		maps.add(pgs);
	}

	@Override
	public void run(){
		// TODO Auto-generated method stub
		try {
			Experimenter.runExperiments(bots, maps, utt, 50, 5000, 300, false, out, -1, true, true,
	        		 false, "");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * maps.add(PhysicalGameState.load("maps/8x8/basesWorkers8x8A.xml",utt));
	 * maps.add(PhysicalGameState.load("maps/16x16/basesWorkers16x16A.xml",utt));
	 * maps.add(PhysicalGameState.load("maps/BWDistantResources32x32.xml",utt));
	 * maps.add(PhysicalGameState.load("maps/8x8/FourBasesWorkers8x8.xml",utt));
	 * maps.add(PhysicalGameState.load("maps/16x16/TwoBasesBarracks16x16.xml",utt));
	 * maps.add(PhysicalGameState.load("maps/NoWhereToRun9x8.xml",utt));
	 * maps.add(PhysicalGameState.load("maps/DoubleGame24x24.xml",utt));
	 * maps.add(PhysicalGameState.load("maps/GardenOfWar64x64.xml",utt));
	 * maps.add(PhysicalGameState.load("maps/barricades24x24.xml",utt));
	 * maps.add(PhysicalGameState.load("maps/12x12/basesWorkers12x12A.xml",utt));
	 */
	
	/*
	 * Experimenter.runExperimentsPartiallyObservable(bots, maps, utt, 50, 3000,
	 * 300, false, out, -1, true, true, false, false, ""); // 8x8 time limit 3000
	 * Experimenter.runExperimentsPartiallyObservable(bots, maps, utt, 50, 4000,
	 * 300, false, out, -1, true, true, false, false, ""); // 16x16 time limit 4000
	 * Experimenter.runExperimentsPartiallyObservable(bots, maps, utt, 50, 5000,
	 * 300, false, out, -1, true, true, false, false, ""); // 24x24 time limit 5000
	 * Experimenter.runExperimentsPartiallyObservable(bots, maps, utt, 50, 6000,
	 * 300, false, out, -1, true, true, false, false, ""); // 32x32 time limit 6000
	 * Experimenter.runExperimentsPartiallyObservable(bots, maps, utt, 50, 8000,
	 * 300, false, out, -1, true, true, false, false, ""); // 64x64 time limit 8000
	 */	

}

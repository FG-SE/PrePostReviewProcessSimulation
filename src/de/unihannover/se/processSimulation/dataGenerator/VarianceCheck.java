package de.unihannover.se.processSimulation.dataGenerator;

import de.unihannover.se.processSimulation.common.ReviewMode;

public class VarianceCheck {

    public static void main(String[] args) {
        BulkParameterFactory f = BulkParameterFactory.forCommercial();
        for (int i = 1; i <= 200; i++) {
            final ExperimentResult resultPost = DataGenerator.runExperiment(f, ReviewMode.POST_COMMIT, false, "post_vc" + f.getSeed());
            final ExperimentResult resultPre = DataGenerator.runExperiment(f, ReviewMode.PRE_COMMIT, false, "pre_vc" + f.getSeed());
            final CombinedResult combined = new CombinedResult(resultPost, resultPre);

            System.out.print(resultPost.getFinishedStoryPoints());
            System.out.print('\t');
            System.out.print(resultPre.getFinishedStoryPoints());
            System.out.print('\t');
            System.out.print(combined.getFinishedStoryPointDiff());
            System.out.print('\t');
            System.out.println(i);

            f = f.copyWithChangedSeed();
        }
    }

}

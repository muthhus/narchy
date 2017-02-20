/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */

package nars.plugin.misc;

import nars.NAR;
import nars.config.Parameters;
import nars.util.Plugin;

/**
 *
 * @author tc
 */
public class RuntimeNARSettings implements Plugin {

    NAR n;
    @Override
    public boolean setEnabled(NAR n, boolean enabled) {
        this.n=n;
        return true;
    }

    public static boolean isImmediateEternalization() {
        return Parameters.IMMEDIATE_ETERNALIZATION;
    }
    public static void setImmediateEternalization(boolean val) {
        Parameters.IMMEDIATE_ETERNALIZATION=val;
    }
    
    public double getDuration() {
        return n.param.duration.get();
    }
    public void setDuration(double val) {
        n.param.duration.set((int) val);
    }
    
    public static double getDerivationPriorityLeak() {
        return Parameters.DERIVATION_PRIORITY_LEAK;
    }
    public static void setDerivationPriorityLeak(double val) {
        Parameters.DERIVATION_PRIORITY_LEAK=(float) val;
    }
    
    public static double getDerivationDurabilityLeak() {
        return Parameters.DERIVATION_DURABILITY_LEAK;
    }
    public static void setDerivationDurabilityLeak(double val) {
        Parameters.DERIVATION_DURABILITY_LEAK=(float) val;
    }

    
    public static double getEvidentalHorizon() {
        return Parameters.HORIZON;
    }
    public static void setEvidentalHorizon(double val) {
        Parameters.HORIZON=(float) val;
    }
    
    public static boolean isInductionOnSucceedingEvents() {
        return Parameters.TEMPORAL_INDUCTION_ON_SUCCEEDING_EVENTS;
    }
    
    public static void setInductionOnSucceedingEvents(boolean val) {
        Parameters.TEMPORAL_INDUCTION_ON_SUCCEEDING_EVENTS=val;
    }

    public static double getInductionSamples() {
        return Parameters.TEMPORAL_INDUCTION_SAMPLES;
    }
    public static void setInductionSamples(double val) {
        Parameters.TEMPORAL_INDUCTION_SAMPLES=(int) val;
    }
    
    public static double getCuriosityDesireConfidenceMul() {
        return Parameters.CURIOSITY_DESIRE_CONFIDENCE_MUL;
    }
    public static void setCuriosityDesireConfidenceMul(double val) {
        Parameters.CURIOSITY_DESIRE_CONFIDENCE_MUL=(float) val;
    }
    
    public static double getCuriosityDesirePriorityMul() {
        return Parameters.CURIOSITY_DESIRE_PRIORITY_MUL;
    }
    public static void setCuriosityDesirePriorityMul(double val) {
        Parameters.CURIOSITY_DESIRE_PRIORITY_MUL=(float)val;
    }
    
    public static double getCuriosityDesireDurabilityMul() {
        return Parameters.CURIOSITY_DESIRE_DURABILITY_MUL;
    }
    public static void setCuriosityDesireDurabilityMul(double val) {
        Parameters.CURIOSITY_DESIRE_DURABILITY_MUL=(float) val;
    }
    
    public static double getCuriosityBusinessThreshold() {
        return Parameters.CURIOSITY_BUSINESS_THRESHOLD;
    }
    public static void setCuriosityBusinessThreshold(double val) {
        Parameters.CURIOSITY_BUSINESS_THRESHOLD=(float) val;
    }
    
    public static boolean isCuriosityForOperatorOnly() {
        return Parameters.CURIOSITY_FOR_OPERATOR_ONLY;
    }
    public static void setCuriosityForOperatorOnly(boolean val) {
        Parameters.CURIOSITY_FOR_OPERATOR_ONLY=val;
    }
    
    
    public static double getHappyEventHigherThreshold() {
        return Parameters.HAPPY_EVENT_HIGHER_THRESHOLD;
    }
    public static void setHappyEventHigherThreshold(double val) {
        Parameters.HAPPY_EVENT_HIGHER_THRESHOLD=(float) val;
    }
    
    public static double getHappyEventLowerThreshold() {
        return Parameters.HAPPY_EVENT_LOWER_THRESHOLD;
    }
    public static void setHappyEventLowerThreshold(double val) {
        Parameters.HAPPY_EVENT_LOWER_THRESHOLD=(float) val;
    }
    
    public static double getBusyEventHigherThreshold() {
        return Parameters.BUSY_EVENT_HIGHER_THRESHOLD;
    }
    public static void setBusyEventHigherThreshold(double val) {
        Parameters.BUSY_EVENT_HIGHER_THRESHOLD=(float) val;
    }
    
   public static double getBusyEventLowerThreshold() {
        return Parameters.BUSY_EVENT_LOWER_THRESHOLD;
    }
    public static void setBusyEventLowerThreshold(double val) {
        Parameters.BUSY_EVENT_LOWER_THRESHOLD=(float) val;
    }
    
    public static boolean isReflectMetaHappyGoal() {
        return Parameters.REFLECT_META_HAPPY_GOAL;
    }
    public static void setReflectMetaHappyGoal(boolean val) {
        Parameters.REFLECT_META_HAPPY_GOAL=val;
    }
    
    public static boolean isUsingConsiderRemind() {
        return Parameters.CONSIDER_REMIND;
    }
    public static void setUsingConsiderRemind(boolean val) {
        Parameters.CONSIDER_REMIND=val;
    }
    
    public static boolean isQuestionGenerationOnDecisionMaking() {
        return Parameters.QUESTION_GENERATION_ON_DECISION_MAKING;
    }
    public static void setQuestionGenerationOnDecisionMaking(boolean val) {
        Parameters.QUESTION_GENERATION_ON_DECISION_MAKING=val;
    }
    
    public static boolean isDecisionQuestionGen() {
        return Parameters.QUESTION_GENERATION_ON_DECISION_MAKING;
    }
    public static void setDecisionQuestionGen(boolean val) {
        Parameters.QUESTION_GENERATION_ON_DECISION_MAKING=val;
    }
    
    public static boolean isHowQuestionGenerationOnDecisionMaking() {
        return Parameters.HOW_QUESTION_GENERATION_ON_DECISION_MAKING;
    }
    public static void setHowQuestionGenerationOnDecisionMaking(boolean val) {
        Parameters.HOW_QUESTION_GENERATION_ON_DECISION_MAKING=val;
    }
    
    public static boolean isCuriosityAlsoOnLowConfidentHighPriorityBelief() {
        return Parameters.CURIOSITY_ALSO_ON_LOW_CONFIDENT_HIGH_PRIORITY_BELIEF;
    }
    public static void setCuriosityAlsoOnLowConfidentHighPriorityBelief(boolean val) {
        Parameters.CURIOSITY_ALSO_ON_LOW_CONFIDENT_HIGH_PRIORITY_BELIEF=val;
    }
    
    public static double getCuriosityPriorityThreshold() {
        return Parameters.CURIOSITY_PRIORITY_THRESHOLD;
    }
    public static void setCuriosityPriorityThreshold(double val) {
        Parameters.CURIOSITY_PRIORITY_THRESHOLD=(float) val;
    }
    
    public static double getCuriosityConfidenceThreshold() {
        return Parameters.CURIOSITY_CONFIDENCE_THRESHOLD;
    }
    public static void setCuriosityConfidenceThreshold(double val) {
        Parameters.CURIOSITY_CONFIDENCE_THRESHOLD=(float) val;
    }
    
    public static double getAnticipationConfidence() {
        return Parameters.ANTICIPATION_CONFIDENCE;
    }
    public static void setAnticipationConfidence(double val) {
        Parameters.ANTICIPATION_CONFIDENCE=(float) val;
    }
    
    public static double getSatisfactionThreshold() {
        return Parameters.SATISFACTION_TRESHOLD;
    }
    public static void setSatisfactionThreshold(double val) {
        Parameters.SATISFACTION_TRESHOLD=(float) val;
    }
}

package submit;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;
import java.util.*;

/**
 * Skeleton class for implementing the Flow.Solver interface.
 */
public class MySolver implements Flow.Solver {

    protected Flow.Analysis analysis;

    /**
     * Sets the analysis.  When visitCFG is called, it will
     * perform this analysis on a given CFG.
     *
     * @param analyzer The analysis to run
     */
    public void registerAnalysis(Flow.Analysis analyzer) {
        this.analysis = analyzer;
    }

    /**
     * Runs the solver over a given control flow graph.  Prior
     * to calling this, an analysis must be registered using
     * registerAnalysis
     *
     * @param cfg The control flow graph to analyze.
     */
    public void visitCFG(ControlFlowGraph cfg) {

        // this needs to come first.
        analysis.preprocess(cfg);

        /***********************
         * Your code goes here *
         ***********************/
        //init out
        QuadIterator qit = new QuadIterator(cfg);
        ArrayList<Quad> entryPoints = new ArrayList<Quad>();
        ArrayList<Quad> exitPoints = new ArrayList<Quad>();
        while (qit.hasNext()) {
            Quad quad = qit.next();
            if (qit.predecessors1().contains(null))
                entryPoints.add(quad);
            if (qit.successors1().contains(null))
                exitPoints.add(quad);
        }
        Flow.DataflowObject entry = analysis.getEntry();
        Flow.DataflowObject exit = analysis.getExit();

        if (analysis.isForward()){
            for (Quad quad: entryPoints){
                analysis.setIn(quad, entry);
            }
            boolean someInChanged = true;
            while(someInChanged){
                someInChanged = false;
                qit = new QuadIterator(cfg, true);
                while(qit.hasNext()){
                    Quad quad = qit.next();
                    Collection<Quad> preds = qit.predecessors1();
                    Flow.DataflowObject in = analysis.getIn(quad);
                    Iterator<Quad> it = preds.iterator();
                    if (preds.contains(null)){
                        preds.remove(null);
                        it = preds.iterator();
                    }else{
                        in = analysis.getOut(it.next());
                    }
                    while(it.hasNext()){
                        Quad q = it.next();
                        in.meetWith(analysis.getOut(q));
                    }

                    analysis.setIn(quad, in);
                    Flow.DataflowObject out_before = analysis.getOut(quad);
                    analysis.processQuad(quad);
                    Flow.DataflowObject out_after = analysis.getOut(quad);
                    if (!out_before.equals(out_after))
                        someInChanged = true;
                }
            }
            Flow.DataflowObject exit_in = analysis.getOut(exitPoints.get(0));
            for (int i = 1; i < exitPoints.size(); ++i)
                exit_in.meetWith(analysis.getOut(exitPoints.get(i)));
            analysis.setExit(exit_in);

        }else{
            //init
            for (Quad quad: exitPoints){
                analysis.setOut(quad, exit);
            }

            boolean someInChanged = true;
            while(someInChanged){
                someInChanged = false;
                qit = new QuadIterator(cfg, false);
                while(qit.hasPrevious()){
                    Quad quad = qit.previous();
                    Collection<Quad> succs = qit.successors1();
                    Flow.DataflowObject out = analysis.getOut(quad);
                    Iterator<Quad> it = succs.iterator();
                    if (succs.contains(null)){
                        succs.remove(null);
                        it = succs.iterator();
                    }else{
                        out = analysis.getIn(it.next());
                    }
                    while(it.hasNext()){
                        Quad q = it.next();
                        out.meetWith(analysis.getIn(q));
                    }
                    analysis.setOut(quad, out);
                    Flow.DataflowObject in_before = analysis.getIn(quad);
                    analysis.processQuad(quad);
                    Flow.DataflowObject in_after = analysis.getIn(quad);
                    if (!in_before.equals(in_after))
                        someInChanged = true;
                }
            }
            Flow.DataflowObject entry_out = analysis.getIn(entryPoints.get(0));
            for (int i = 1; i <  entryPoints.size(); ++i){
                entry_out.meetWith(analysis.getIn(entryPoints.get(i)));
            }
            analysis.setEntry(entry_out);
        }

        // this needs to come last.
        analysis.postprocess(cfg);
    }
}

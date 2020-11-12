package submit;

import flow.Flow;
import joeq.Compiler.BytecodeAnalysis.Bytecodes;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static joeq.Compiler.Quad.Operator.Binary.getDest;
import static joeq.Compiler.Quad.Operator.Binary.getSrc2;
import static joeq.Compiler.Quad.Operator.IntIfCmp.*;

public class HandleNullCheck implements Flow.Analysis {

    private VarSet[] in, out;
    private VarSet entry, exit;
    public  TreeMap<Integer, TreeMap<Integer, VarSet>> branchOut = new TreeMap<Integer, TreeMap<Integer, VarSet>>();
    private  int optLevel = 0;
    public HandleNullCheck(int optLevel){
        this.optLevel = optLevel;
    }
    public int getOptLevel(){
        return optLevel;
    }
    public void preprocess(ControlFlowGraph cfg) {
        if (0 == optLevel)
            System.out.print(cfg.getMethod().getName().toString() + " ");
        /* Generate initial conditions. */
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int x = qit.next().getID();
            if (x > max) max = x;
        }
        max += 1;
        in = new VarSet[max];
        out = new VarSet[max];
        qit = new QuadIterator(cfg);

        Set<String> s = new TreeSet<String>();
        VarSet.universalSet = s;

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            s.add("R" + i);
        }

        while (qit.hasNext()) {
            Quad q = qit.next();
            for (RegisterOperand def : q.getDefinedRegisters()) {
                s.add(def.getRegister().toString());
            }
            for (RegisterOperand use : q.getUsedRegisters()) {
                s.add(use.getRegister().toString());
            }


            if (optLevel == 2) {
                if (q.getOperator() instanceof Operator.IntIfCmp.IFCMP_A) {
                    if (IntIfCmp.getSrc2(q).toString() .equals( "AConst: null")){
                        Integer qi;
                        boolean eq = Operator.IntIfCmp.getCond(q).getCondition() == 0;

                        if (!eq)
                            qi = IntIfCmp.getTarget(q).getTarget().getQuad(0).getID();
                        else
                            qi = qit.getCurrentBasicBlock().getFallthroughSuccessor().getQuad(0).getID();

                        if (!branchOut.containsKey(qi))
                            branchOut.put(qi, new TreeMap<Integer, VarSet>());
                        if (!branchOut.get(qi).containsKey(q.getID()))
                            branchOut.get(qi).put(q.getID(), new VarSet());

                        branchOut.get(qi).get(q.getID()).set.add(((RegisterOperand)(IntIfCmp.getSrc1(q))).getRegister().toString());
                    }
                }
            }
        }

        entry = new VarSet();
        exit = new VarSet();
        entry.setToBottom();
        for (int i = 0; i < in.length; i++) {
            in[i] = new VarSet();
            out[i] = new VarSet();
        }



    }

    public void postprocess(ControlFlowGraph cfg) {
        QuadIterator qit = new QuadIterator(cfg);
        while(qit.hasNext()){
            Quad q = qit.next();
            if (q.getOperator() instanceof Operator.NullCheck.NULL_CHECK){
                if (in[q.getID()].contains(q.getUsedRegisters().get(0).getRegister().toString())){
                    if (optLevel > 0)
                        qit.remove();
                    else
                        System.out.print(q.getID() + " ");
                }
            }
        }
        if (optLevel == 0)
            System.out.println();
    }

    /* Is this a forward dataflow analysis? */
    public boolean isForward() {
        return true;
    }

    /* Routines for interacting with dataflow values. */

    public Flow.DataflowObject getEntry() {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }

    public void setEntry(Flow.DataflowObject value) {
        entry.copy(value);
    }

    public Flow.DataflowObject getExit() {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }

    public void setExit(Flow.DataflowObject value) {
        exit.copy(value);
    }

    public Flow.DataflowObject getIn(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]);
        return result;
    }

    public Flow.DataflowObject getOut(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]);
        return result;
    }

    public void setIn(Quad q, Flow.DataflowObject value) {
        in[q.getID()].copy(value);
    }

    public void setOut(Quad q, Flow.DataflowObject value) {
        out[q.getID()].copy(value);
    }

    public Flow.DataflowObject newTempVar() {
        return new VarSet();
    }

    /* Actually perform the transfer operation on the relevant
     * quad. */

    public void processQuad(Quad q) {
        VarSet o = new VarSet();
        o.copy(in[q.getID()]);
        for (RegisterOperand def : q.getDefinedRegisters()) {
            o.killVar(def.getRegister().toString());
        }

        if (q.getOperator() instanceof Operator.NullCheck.NULL_CHECK) {
            for (RegisterOperand use : q.getUsedRegisters()) {
                o.genVar(use.getRegister().toString());
            }
        }

        if (optLevel == 2) {
            if (q.getOperator() instanceof Operator.New || q.getOperator() instanceof Operator.NewArray) {
                for (RegisterOperand use : q.getUsedRegisters()) {
                    o.genVar(use.getRegister().toString());
                }
            }
        }



        out[q.getID()].copy(o);
    }

    public static class VarSet implements Flow.DataflowObject {
        public static Set<String> universalSet;
        private Set<String> set;

        public VarSet() {
            set = new TreeSet<String>(universalSet);
        }

        public VarSet with(VarSet v){
            set.addAll(v.set);
            return this;
        }

        public boolean contains(String v){
            return set.contains(v);
        }

        public void setToTop() {
            set = new TreeSet<String>(universalSet);
        }

        public void setToBottom() {
            set = new TreeSet<String>();
        }

        public void meetWith(Flow.DataflowObject o) {
            VarSet a = (VarSet) o;
            set.retainAll(a.set);
        }

        public void copy(Flow.DataflowObject o) {
            VarSet a = (VarSet) o;
            set = new TreeSet<String>(a.set);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof VarSet) {
                VarSet a = (VarSet) o;
                return set.equals(a.set);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }

        @Override
        public String toString() {
            return set.toString();
        }

        public void genVar(String v) {
            set.add(v);
        }

        public void killVar(String v) {
            set.remove(v);
        }
    }

    /* The QuadVisitor that actually does the computation */
}

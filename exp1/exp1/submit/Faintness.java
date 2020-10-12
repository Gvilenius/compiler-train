package submit;

import java.util.*;

import flow.Flow;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Main.Helper;

public class Faintness implements Flow.Analysis {

    public static class VarSet implements Flow.DataflowObject {
        private Set<String> set;
        public static Set<String> universalSet;
        public VarSet() { set = new TreeSet<String>(); }
        public boolean contains(String v){ return set.contains(v);}
        public void setToTop() { set = new TreeSet<String>(); }
        public void setToBottom() { set = new TreeSet<String>(universalSet); }

        public void meetWith(Flow.DataflowObject o)
        {
            VarSet a = (VarSet)o;
            set.retainAll(a.set);
        }

        public void copy(Flow.DataflowObject o)
        {
            VarSet a = (VarSet) o;
            set = new TreeSet<String>(a.set);
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof VarSet)
            {
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
        public String toString()
        {
            return set.toString();
        }

        public void genVar(String v) {set.add(v);}
        public void killVar(String v) {set.remove(v);}
    }

    private VarSet[] in, out;
    private VarSet entry, exit;

    public void preprocess(ControlFlowGraph cfg) {
        System.out.println("Method: "+cfg.getMethod().getName().toString());
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
            s.add("R"+i);
        }

        while (qit.hasNext()) {
            Quad q = qit.next();
            for (RegisterOperand def : q.getDefinedRegisters()) {
                s.add(def.getRegister().toString());
            }
            for (RegisterOperand use : q.getUsedRegisters()) {
                s.add(use.getRegister().toString());
            }
        }

        entry = new VarSet();
        exit = new VarSet();
        exit.setToBottom();

        transferfn.val = new VarSet();
        for (int i=0; i<in.length; i++) {
            in[i] = new VarSet();
            out[i] = new VarSet();
        }

        System.out.println("Initialization completed.");
    }

    public void postprocess(ControlFlowGraph cfg) {
        System.out.println("entry: "+entry.toString());
        for (int i=1; i<in.length; i++) {
            System.out.println(i+" in:  "+in[i].toString());
            System.out.println(i+" out: "+out[i].toString());
        }
        System.out.println("exit: "+exit.toString());
    }

    /* Is this a forward dataflow analysis? */
    public boolean isForward() { return false; }

    /* Routines for interacting with dataflow values. */

    public Flow.DataflowObject getEntry()
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }
    public Flow.DataflowObject getExit()
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }
    public Flow.DataflowObject getIn(Quad q)
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]);
        return result;
    }
    public Flow.DataflowObject getOut(Quad q)
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]);
        return result;
    }
    public void setIn(Quad q, Flow.DataflowObject value)
    {
        in[q.getID()].copy(value);
    }
    public void setOut(Quad q, Flow.DataflowObject value)
    {
        out[q.getID()].copy(value);
    }
    public void setEntry(Flow.DataflowObject value)
    {
        entry.copy(value);
    }
    public void setExit(Flow.DataflowObject value)
    {
        exit.copy(value);
    }

    public Flow.DataflowObject newTempVar() { return new VarSet(); }

    /* Actually perform the transfer operation on the relevant
     * quad. */

    private TransferFunction transferfn = new TransferFunction();
    public void processQuad(Quad q) {
        transferfn.val.copy(out[q.getID()]);
        Helper.runPass(q, transferfn);
        in[q.getID()].copy(transferfn.val);
    }

    /* The QuadVisitor that actually does the computation */
    public class TransferFunction extends QuadVisitor.EmptyVisitor {
        VarSet val;
        @Override
        public void visitMove(Quad q) {
            String dst = Operator.Move.getDest(q).getRegister().toString();
            if (!out[q.getID()].contains(dst)){
                visitQ(q);
            }else {
                for (RegisterOperand def : q.getDefinedRegisters()) {
                    val.genVar(def.getRegister().toString());
                }
            }

        }

        @Override
        public void visitBinary(Quad q) {
            String dst = Operator.Binary.getDest(q).getRegister().toString();
            if (!out[q.getID()].contains(dst)){
                visitQ(q);
            }else{
                for (RegisterOperand def : q.getDefinedRegisters()) {
                    val.genVar(def.getRegister().toString());
                }
            }
        }

        @Override
        public void visitUnary(Quad q) {
            visitQ(q);
        }

        @Override
        public void visitALoad(Quad q) {
            visitQ(q);
        }

        @Override
        public void visitALength(Quad q) {
            visitQ(q);
        }

        @Override
        public void visitGetstatic(Quad q) {
            visitQ(q);
        }
        @Override
        public void visitNullCheck(Quad q) {
            visitQ(q);
        }

        @Override
        public void visitGetfield(Quad q) {
            visitQ(q);
        }

        @Override
        public void visitInstanceOf(Quad q) {
            visitQ(q);
        }

        @Override
        public void visitNew(Quad q) {
            visitQ(q);
        }

        @Override
        public void visitNewArray(Quad q) {
            visitQ(q);
        }

        @Override
        public void visitInvoke(Quad q) {
            visitQ(q);
        }

        @Override
        public void visitJsr(Quad q) {
            visitQ(q);
        }

        @Override
        public void visitCheckCast(Quad q) {
            visitQ(q);
        }
        @Override
        public void visitReturn(Quad q){
            visitQ(q);
        }
        public void visitQ(Quad q){
            for (RegisterOperand use : q.getUsedRegisters()) {
                val.killVar(use.getRegister().toString());
            }
            for (RegisterOperand def : q.getDefinedRegisters()) {
                val.genVar(def.getRegister().toString());
            }
        }
    }
}

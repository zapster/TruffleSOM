package trufflesom.primitives.reflection;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinarySystemOperation;
import trufflesom.vm.Globals;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Primitive(className = "System", primitive = "hasGlobal:")
public abstract class HasGlobalPrim extends BinarySystemOperation {

  @Child private HasGlobalNode hasGlobal;

  @Override
  public BinarySystemOperation initialize(final Universe universe) {
    super.initialize(universe);
    hasGlobal = new UninitializedHasGlobal(0, universe);
    return this;
  }

  @Specialization(guards = "receiver == universe.getSystemObject()")
  public final boolean doSObject(final SObject receiver, final SSymbol argument) {
    return hasGlobal.hasGlobal(argument);
  }

  private abstract static class HasGlobalNode extends Node {
    protected static final int INLINE_CACHE_SIZE = 6;

    public abstract boolean hasGlobal(SSymbol argument);
  }

  private static final class UninitializedHasGlobal extends HasGlobalNode {
    private final int      depth;
    private final Universe universe;

    UninitializedHasGlobal(final int depth, final Universe universe) {
      this.depth = depth;
      this.universe = universe;
    }

    @Override
    @TruffleBoundary
    public boolean hasGlobal(final SSymbol argument) {
      boolean hasGlobal = Globals.hasGlobal(argument);

      if (hasGlobal) {
        return specialize(argument).hasGlobal(argument);
      }
      return false;
    }

    private HasGlobalNode specialize(final SSymbol argument) {
      if (depth < INLINE_CACHE_SIZE) {
        return replace(new CachedHasGlobal(argument, depth, universe));
      } else {
        HasGlobalNode head = this;
        while (head.getParent() instanceof HasGlobalNode) {
          head = (HasGlobalNode) head.getParent();
        }
        return head.replace(new HasGlobalFallback());
      }
    }
  }

  private static final class CachedHasGlobal extends HasGlobalNode {
    private final int            depth;
    private final SSymbol        name;
    @Child private HasGlobalNode next;

    CachedHasGlobal(final SSymbol name, final int depth, final Universe universe) {
      this.depth = depth;
      this.name = name;
      next = new UninitializedHasGlobal(this.depth + 1, universe);
    }

    @Override
    public boolean hasGlobal(final SSymbol argument) {
      if (name == argument) {
        return true;
      } else {
        return next.hasGlobal(argument);
      }
    }
  }

  private static final class HasGlobalFallback extends HasGlobalNode {
    @Override
    public boolean hasGlobal(final SSymbol argument) {
      return Globals.hasGlobal(argument);
    }
  }
}

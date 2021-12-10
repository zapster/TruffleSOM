package trufflesom.primitives.reflection;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.nodes.PreevaluatedExpression;
import bd.source.SourceCoordinate;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.primitives.arrays.ToArgumentsArrayNode;
import trufflesom.primitives.arrays.ToArgumentsArrayNodeFactory;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public abstract class AbstractSymbolDispatch extends Node {
  public static final int INLINE_CACHE_SIZE = 6;

  private final long sourceCoord;

  public AbstractSymbolDispatch(final long coord) {
    assert coord != 0;
    this.sourceCoord = coord;
  }

  @Override
  public final SourceSection getSourceSection() {
    return SourceCoordinate.createSourceSection(this, sourceCoord);
  }

  public abstract Object executeDispatch(VirtualFrame frame, Object receiver,
      SSymbol selector, Object argsArr);

  protected final AbstractMessageSendNode createForPerformNodes(final SSymbol selector) {
    return MessageSendNode.createForPerformNodes(selector, sourceCoord);
  }

  public static final ToArgumentsArrayNode createArgArrayNode() {
    return ToArgumentsArrayNodeFactory.create(null, null);
  }

  @Specialization(limit = "INLINE_CACHE_SIZE",
      guards = {"selector == cachedSelector", "argsArr == null"})
  public Object doCachedWithoutArgArr(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final Object argsArr,
      @Cached("selector") final SSymbol cachedSelector,
      @Cached("createForPerformNodes(selector)") final AbstractMessageSendNode cachedSend) {
    Object[] arguments = {receiver};

    PreevaluatedExpression realCachedSend = cachedSend;
    return realCachedSend.doPreEvaluated(frame, arguments);
  }

  @Specialization(limit = "INLINE_CACHE_SIZE", guards = "selector == cachedSelector")
  public Object doCached(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final SArray argsArr,
      @Cached("selector") final SSymbol cachedSelector,
      @Cached("createForPerformNodes(selector)") final AbstractMessageSendNode cachedSend,
      @Cached("createArgArrayNode()") final ToArgumentsArrayNode toArgArray) {
    Object[] arguments = toArgArray.executedEvaluated(argsArr, receiver);

    PreevaluatedExpression realCachedSend = cachedSend;
    return realCachedSend.doPreEvaluated(frame, arguments);
  }

  @TruffleBoundary
  @Specialization(replaces = "doCachedWithoutArgArr", guards = "argsArr == null")
  public Object doUncached(final Object receiver, final SSymbol selector, final Object argsArr,
      @Cached("create()") final IndirectCallNode call) {
    SInvokable invokable = Types.getClassOf(receiver).lookupInvokable(selector);

    Object[] arguments = {receiver};

    return call.call(invokable.getCallTarget(), arguments);
  }

  @TruffleBoundary
  @Specialization(replaces = "doCached")
  public Object doUncached(final Object receiver, final SSymbol selector, final SArray argsArr,
      @Cached("create()") final IndirectCallNode call,
      @Cached("createArgArrayNode()") final ToArgumentsArrayNode toArgArray) {
    SInvokable invokable = Types.getClassOf(receiver).lookupInvokable(selector);

    Object[] arguments = toArgArray.executedEvaluated(argsArr, receiver);

    return call.call(invokable.getCallTarget(), arguments);
  }
}

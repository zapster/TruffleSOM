package trufflesom.primitives.reflection;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "perform:inSuperclass:")
public abstract class PerformInSuperclassPrim extends TernaryExpressionNode {

  @Specialization
  @TruffleBoundary
  public static final Object doSAbstractObject(final SAbstractObject receiver,
      final SSymbol selector, final SClass clazz,
      @Cached final IndirectCallNode call) {
    CompilerAsserts.neverPartOfCompilation("PerformInSuperclassPrim");
    SInvokable invokable = clazz.lookupInvokable(selector);
    return call.call(invokable.getCallTarget(), new Object[] {receiver});
  }
}

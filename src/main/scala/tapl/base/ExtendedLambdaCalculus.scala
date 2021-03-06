package tapl.base

object ExtendedLambdaCalculus {

  object traits extends Terms{ type T = Term }

  /**
    * Not sealed so we can extend further.
    */
  abstract class Term extends traits.Rewritable {
    def apply(arg: Term): Term = App(this, arg)
  }

  case class BoolLiteral(value: Boolean) extends Term with traits.Literal[Boolean] {
    override def replicateUsing(mapping: traits.Mapping): Term = BoolLiteral(value)
  }

  case class IntLiteral(value: Int) extends Term with traits.Literal[Int] {
    override def replicateUsing(mapping: traits.Mapping): Term = IntLiteral(value)
  }

  case class Var(name: String) extends Term with traits.Var {
    override def replicateUsing(mapping: traits.Mapping = Map()): Var = Var(name)
  }

  case class App(fun: Term, arg: Term) extends Term with traits.App {
    override def replicateUsing(mapping: traits.Mapping): Term =
      App(fun.rewriteUsing(mapping), arg.rewriteUsing(mapping))
  }

  case class Abs(variable: Var, body: Term) extends Term with traits.Abs {
    override def replicateUsing(mapping: traits.Mapping): Abs = {
      val varCopy = variable.replicateUsing()
      Abs(varCopy, body.rewriteUsing(mapping + (variable -> varCopy)))
    }
  }

  case class If(guard: Term, trueBranch: Term, falseBranch: Term) extends Term {
    override def replicateUsing(mapping: traits.Mapping): Term =
      If(guard.rewriteUsing(mapping),
        trueBranch.rewriteUsing(mapping),
        falseBranch.rewriteUsing(mapping))

    override def toString: String = s"if ($guard) then $trueBranch else $falseBranch"
  }

  case class Let(variable: Var, init: Term, body: Term) extends Term {
    override def replicateUsing(mapping: traits.Mapping): Term = {
      val varCopy = variable.replicateUsing()
      Let(varCopy, init.rewriteUsing(mapping), body.rewriteUsing(mapping + (variable -> varCopy)))
    }
    override def toString: String = s"let $variable = $init in $body"
  }

  def lambda(name: String)(body: Var => Term): Abs = {
    val v = Var(name)
    Abs(v, body(v))
  }

  def let(name: String, init: Term)(body: Var => Term): Let = {
    val variable = Var(name)
    Let(variable, init, body(variable))
  }

  def eval1(term: Term): Option[Term] = term match {
    case App(Abs(v, body), arg) if arg.isValue => Some(body.rewriteUsing(Map(v -> arg)))
    case App(f, arg) if f.isValue => eval1(arg).map(App(f, _))
    case App(f, arg) => eval1(f).map(App(_, arg))
    case If(BoolLiteral(true), tb, _) => Some(tb)
    case If(BoolLiteral(false), _, fb) => Some(fb)
    case If(x, tb, fb) if !x.isValue => eval1(x).map(If(_, tb, fb))
    case Let(v, init, body) if init.isValue => Some(body.rewriteUsing(Map(v -> init)))
    case Let(v, init, body) => eval1(init).map(If(v, _, body))
    case _ => None
  }
}

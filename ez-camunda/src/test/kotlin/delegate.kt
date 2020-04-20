import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate

class Printer:JavaDelegate{
    override fun execute(p0: DelegateExecution?) {
        println("${this::class.java} of JavaDelegate called")
    }
}
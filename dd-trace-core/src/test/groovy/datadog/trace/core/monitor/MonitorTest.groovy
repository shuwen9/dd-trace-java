package datadog.trace.core.monitor

import com.timgroup.statsd.StatsDClient
import datadog.trace.api.sampling.PrioritySampling
import datadog.trace.common.writer.DDAgentWriter
import datadog.trace.common.writer.ddagent.DDAgentApi
import datadog.trace.util.test.DDSpecification
import spock.lang.Ignore
import spock.lang.Subject

import java.util.concurrent.ThreadLocalRandom

class MonitorTest extends DDSpecification {
  def statsD = Mock(StatsDClient)

  @Subject
  def monitor = new Monitor(statsD)

  // This fails because DDAgentWriter isn't an interface and the mock doesn't prevent the call.
  @Ignore
  def "test onStart"() {
    setup:
    def writer = Mock(DDAgentWriter)

    when:
    monitor.onStart(writer)

    then:
    1 * writer.getCapacity() >> capacity
    0 * _

    where:
    capacity = ThreadLocalRandom.current().nextInt()
  }

  def "test onShutdown"() {
    when:
    monitor.onShutdown(true)

    then:
    0 * _
  }

  def "test onPublish"() {
    when:
    monitor.onPublish(trace, samplingPriority)

    then:
    // verify the tags syntax
    1 * statsD.incrementCounter('queue.accepted',  "priority:" + priorityName )
    1 * statsD.count('queue.accepted_lengths', trace.size())
    0 * _

    where:
    trace          |  samplingPriority                |    priorityName
    []             |   PrioritySampling.USER_DROP     |      "user_drop"
    [null, null]   |   PrioritySampling.USER_DROP     |      "user_drop"
    []             |   PrioritySampling.SAMPLER_KEEP  |   "sampler_keep"
    [null, null]   |   PrioritySampling.SAMPLER_KEEP  |   "sampler_keep"
  }

  def "test onFailedPublish"() {
    when:
    monitor.onFailedPublish(samplingPriority)

    then:
    1 * statsD.incrementCounter('queue.dropped', { it.startsWith("priority:") })
    0 * _

    where:
    samplingPriority << [PrioritySampling.SAMPLER_KEEP, PrioritySampling.USER_KEEP, PrioritySampling.USER_DROP, PrioritySampling.SAMPLER_DROP, PrioritySampling.UNSET]
  }

  def "test onScheduleFlush"() {
    when:
    monitor.onScheduleFlush(true)

    then:
    0 * _
  }

  def "test onFlush"() {
    when:
    monitor.onFlush(true)

    then:
    0 * _
  }

  def "test onSerialize"() {
    when:
    monitor.onSerialize(bytes)

    then:
    1 * statsD.count('queue.accepted_size', bytes)
    0 * _

    where:
    bytes = ThreadLocalRandom.current().nextInt(10000)
  }

  def "test onFailedSerialize"() {
    when:
    monitor.onFailedSerialize(null, null)

    then:
    0 * _
  }

  def "test onSend"() {
    when:
    monitor.onSend(representativeCount, sendSize, response)

    then:
    1 * statsD.incrementCounter('api.requests')
    1 * statsD.recordGaugeValue('queue.length', representativeCount)
    1 * statsD.recordGaugeValue('queue.size', sendSize)
    if (response.exception()) {
      1 * statsD.incrementCounter('api.errors')
    }
    if (response.status()) {
      1 * statsD.incrementCounter('api.responses', ["status:${response.status()}"])
    }
    0 * _

    where:
    response << [
      DDAgentApi.Response.success(ThreadLocalRandom.current().nextInt(1, 100)),
      DDAgentApi.Response.failed(ThreadLocalRandom.current().nextInt(1, 100)),
      DDAgentApi.Response.success(ThreadLocalRandom.current().nextInt(1, 100), new Throwable()),
      DDAgentApi.Response.failed(new Throwable()),
    ]

    representativeCount = ThreadLocalRandom.current().nextInt(1, 100)
    sendSize = ThreadLocalRandom.current().nextInt(1, 100)
  }

  def "test onFailedSend"() {
    when:
    monitor.onFailedSend(representativeCount, sendSize, response)

    then:
    1 * statsD.incrementCounter('api.requests')
    1 * statsD.recordGaugeValue('queue.length', representativeCount)
    1 * statsD.recordGaugeValue('queue.size', sendSize)
    if (response.exception()) {
      1 * statsD.incrementCounter('api.errors')
    }
    if (response.status()) {
      1 * statsD.incrementCounter('api.responses', ["status:${response.status()}"])
    }
    0 * _

    where:
    response << [
      DDAgentApi.Response.success(ThreadLocalRandom.current().nextInt(1, 100)),
      DDAgentApi.Response.failed(ThreadLocalRandom.current().nextInt(1, 100)),
      DDAgentApi.Response.success(ThreadLocalRandom.current().nextInt(1, 100), new Throwable()),
      DDAgentApi.Response.failed(new Throwable()),
    ]

    representativeCount = ThreadLocalRandom.current().nextInt(1, 100)
    sendSize = ThreadLocalRandom.current().nextInt(1, 100)
  }
}

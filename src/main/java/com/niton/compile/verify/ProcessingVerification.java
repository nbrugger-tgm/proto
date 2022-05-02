package com.niton.compile.verify;

import static java.lang.String.format;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import javax.lang.model.element.Element;

import com.niton.compile.processor.ProcessingLogger;

/**
 * A verification for {@link Element}s
 */
public class ProcessingVerification implements Reasonable, Verifiable
{
    private final ProcessingLogger logger;
    private final BooleanSupplier predicate;
    private String message;
    private final Element element;
    private boolean inverted;

    /**
     * Examples for message formatting:
     * <pre>
     * "Controllers should [not] be interfaces"
     *  -> "Controllers should not be interfaces" when called with {@link #not()}
     *  -> "Controllers should be interfaces" when called without {@link #not()}
     * "Controllers should have \\[and\\] in their name"
     *  -> "Controllers should have [and] in their name" regardless of {@link #not()}
     * </pre>
     *
     * @param log       The logger to fail or warn
     * @param predicate The predicate to determine if the element is valid
     * @param message   The message to print if verification is not successful.
     *                  <b>To support inverting the query place negating words in square brackets!</b>
     *                  <p>
     *                  {@code "Controllers should [not] be interfaces} supports negation for example,
     *                  if you need to print [ and ] in your message you can escape them with a single backslash.
     *                  </p>
     * @param element   the element to verify
     */
    public ProcessingVerification(ProcessingLogger log, Predicate<Element> predicate,
        String message, Element element)
    {
        logger = log;
        this.predicate = () -> predicate.test(element);
        this.message = message;
        this.element = element;
    }

    /**
     * Inverts the verification and adapts the message
     * @return this
     */
    public<T extends Reasonable & Verifiable> T not()
    {
        this.inverted = true;
        return (T) this;
    }

    @Override
    public Verifiable because(String message, Object... args)
    {
        this.message = format("%s, because %s", this.message, format(message, args));
        return this;
    }

    @Override
    public boolean failOnViolation()
    {
        var fail = inverted == predicate.getAsBoolean();
        if (fail)
            logger.fail(element, formatMessage());
        return !fail;
    }

    @Override
    public boolean warnOnViolation()
    {
        var fail = inverted == predicate.getAsBoolean();
        if (fail)
            logger.warn(element, formatMessage());
        return !fail;
    }

    @Override
    public boolean infoOnViolation()
    {
        var fail = inverted == predicate.getAsBoolean();
        if (fail)
            logger.info(element, formatMessage());
        return !fail;
    }

    @Override
    public boolean isValid()
    {
        return inverted != predicate.getAsBoolean();
    }

    private String formatMessage()
    {
        if (inverted)
            return unescape(message.replaceAll("(?<!\\\\)[\\[\\]]", ""));//replace all non-escaped brackets
        else
            return unescape(message.replaceAll("(?<!\\\\)\\[.+(?<!\\\\)]",
                "")); //replace all non-escaped brackets and the content between them
    }

    private String unescape(String string)
    {
        return string.replace("\\[", "[").replace("\\]", "]");
    }
}

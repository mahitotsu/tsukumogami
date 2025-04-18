export default defineEventHandler(async (event) => {
    const body = await readBody(event);

    return JSON.stringify({
        prompt: body.prompt,
        answer: new Date().toISOString(),
    });
});
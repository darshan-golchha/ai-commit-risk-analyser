import google.generativeai as genai
import textwrap
import markdown


def to_markdown(text):
    # Display the text in markdown format with **bold** and *italic* text
    # and *<text> for bullet points
    return textwrap.dedent(f"""
    {text}
    """)

# GOOGLE_API_KEY = "AIzaSyA2R1l6piGnF6I7No8js_YwWJEYNgTOslw"
# GOOGLE_API_KEY = "AIzaSyA-1uTQgJK6nXrkCpPwKV2FsssKIzXqz5M"
GOOGLE_API_KEY = "AIzaSyB0US_RutA_cBZDNrA7z5v2E9k9vwtqCSo"
genai.configure(api_key=GOOGLE_API_KEY)
model = genai.GenerativeModel('gemini-1.5-flash')



def get_completion(prompt):
    if prompt == "":
        return "Empty Error Prompt."
    else:
        prompt = f"Give me the Code Review of the diff in \"{prompt}\" . Check for any potneital bugs or flaws that might occure and give me a scale from 1 to 10 as to how severe this could be? Only give me one severity that has taken all factors into consideration and is representative of all kinds of issues together. Also for internal purposes give this severity enclosed within brackets in this format as Example -> [severity: 7/10]. Do not add any text decorations inside the square brackets."
    global model
    try:
        response = to_markdown(model.generate_content(prompt).text)
        return response
    except Exception as e:
        return f"API Retrieval Error, You Error Prompt was:- {prompt}."

# This method returns a list of completions for the error prompts
def solve_error_prompts(error_prompts):
    return [get_completion(prompt) for prompt in error_prompts]
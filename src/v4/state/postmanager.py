from enum import Enum
import json
import random
import time
import datetime

from state.reddit.generator import Generator
from state.reddit.pandasdata import PandasData
from state.reddit.pandasfilter import PandasFilter

from state.kaomoji.kaomoji import KaomojiHelp


class PostType(Enum):
    POST_TYPE_KAOMOJI = 0
    POST_TYPE_GIF = 1
    POST_TYPE_REDDIT = 2
    POST_TYPE_ASCII = 3
    POST_TYPE_EMOJI = 4
    """
    # POST_TYPE_START = -1
    # POST_TYPE_USER = 0
    # POST_TYPE_HANDWRITING = 2
    # POST_TYPE_RNN_NAILS = 3
    # POST_TYPE_NAILS_CITATION = 5
    """


class Post(object):
    def __init__(self, previous):
        self.connection(previous)

        self._timestamp = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d %H:%M:%S')

    def connection(self, previous):
        """
        makes the connection, generates class variables depending
        on what of the previous post to take into account for
        generating the new post
        """
        raise NotImplementedError("Should have implemented this")

    def text(self):
        """
        :return: the text representation of the post
        """
        raise NotImplementedError("Should have implemented this")

    def json(self):
        """
        convert a post to json in order to send it to the frontend
        """
        return json.dumps(self.dict())

    # TODO for what?
    def dict(self):
        """
        returns a dict of the post
        """
        raise NotImplementedError("Should have implemented this")

    def timestamp(self):
        return self._timestamp


# TODO what's this for exactly?
class StartPost(Post):
    def __init__(self, previous, text=""):
        super().__init__(previous)
        self._text = text

    def connection(self, previous):
        pass

    def text(self):
        return self._text

    def json(self):
        pass

    def dict(self):
        pass

# some static variable
kao = KaomojiHelp()


class KaomojiPost(Post):
    def __init__(self, previous):
        self.kaomoji = kao.random()
        super().__init__(previous)

    def connection(self, previous):
        words = previous.text().split()
        self.kaomoji = kao.find(words)
        if not self.kaomoji:
            self.kaomoji = kao.random()
        else:
            print("FOUND A MATCH")

    def text(self):
        return self.kaomoji.kaomoji()

    def dict(self):
        return {
            'user': self.kaomoji.emotions(),
            'text': self.text(),
            'attachment': None,
            'style': 'unformatted',
            'timestamp': self.timestamp()
        }


class GifPost(Post):
    def __init__(self, previous):
        super().__init__(previous)

    def connection(self, previous):
        self._text = previous.text()
        self.path = 'data/gif/1469571231514.gif'

    def text(self):
        return self._text

    def dict(self):
        return {
            'user': 'gif',
            'text': self.path,
            'attachment': None,
            'style': 'unformatted',
            'timestamp': self.timestamp()
        }


# some heavy, static variables
feather_file = 'data/reddit/test_reddit_4chan.feather'
block_words = open('data/reddit/blocked_words.txt').readlines()
block_chars = "".join(open('data/reddit/blocked_chars.txt').readlines())
data = PandasData(feather_file, block_words=block_words)
data.load()
df = data.df
generator = Generator(PandasFilter(df), block_words=block_words, block_chars=block_chars)


class RedditPost(Post):
    def __init__(self, previous):
        """
        uses a class that generates(filters) a new post from reddit/4chan
        this class can be more detailed parametrized. check Generator
        :param previous:
        """
        super().__init__(previous)

        generator.reset()
        generator.clear()
        generator.length(150, 550).shannon_entropy(1.0, 10)
        generator.generate()
        self._text = generator.sentences()[0].text

    def connection(self, previous):
        # TODO
        pass

    def text(self):
        return self._text

    def dict(self):
        return {
            'user': 'reddit/4chan',
            'text': self.text(),
            'attachment': None,
            'style': 'unformatted',
            'timestamp': self.timestamp()
        }


class AsciiPost(Post):
    def __init__(self, previous):
        super().__init__(previous)

    def connection(self, previous):
        self.butterfly = open('data/ascii/butterfly.txt', 'r').readlines()
        self._text = ""
        for line in self.butterfly:
            self._text += line

    def text(self):
        return self._text

    def dict(self):
        return {
            'text': self.text(),
            'user': 'ascii',
            'attachment': None,
            'style': 'formatted',
            'timestamp': self.timestamp()
        }


class EmojiPost(Post):
    def __init__(self, previous):
        super().__init__(previous)

    def connection(self, previous):
        self._text = 'i like :smiley:'

    def text(self):
        return self._text

    def dict(self):
        return {
            'user': 'emojipost',
            'text': self.text(),
            'style': 'unformatted',
            'timestamp': self.timestamp(),
            'attachment': None
        }


class PostManager(object):
    def __init__(self):
        self._max_history = 20
        self.posts = []
        self.posts.append(StartPost(previous=None, text="Start Text"))

    def _limit(self):
        """
        limits the list of posts
        """
        if len(self.posts) > self._max_history:
            self.posts.pop(0)

    def last(self):
        """
        :return: the last added post
        """
        return self.posts[-1]

    def add(self, postType):
        """
        adds a new post
        :param postType: type of the new post
        """
        new = self.get(postType, self.last())
        self.posts.append(new)
        self._limit()

    def add_random(self):
        """
        adds a new random post
        """
        randomType = random.choice(list(PostType))
        new = self.get(randomType, self.last())
        self.posts.append(new)
        self._limit()

    @staticmethod
    def get(ptype, previous):
        """
        returns a new Post() instance
        :param ptype: type of the new post
        :param previous: the previously generated post
        :return: a new post of certain type
        """
        # TODO 5000 ifs a nice but maybe an pythonic enum alternative.
        # TODO a dict with all the PostType as keys and clazzes as values could turn this into one line
        # I like this approach https://www.quora.com/What-is-the-best-way-to-implement-enums-in-Python
        if ptype is PostType.POST_TYPE_KAOMOJI:
            return KaomojiPost(previous=previous)
        if ptype is PostType.POST_TYPE_GIF:
            return GifPost(previous=previous)
        if ptype is PostType.POST_TYPE_REDDIT:
            return RedditPost(previous=previous)
        if ptype is PostType.POST_TYPE_ASCII:
            return AsciiPost(previous=previous)
        if ptype is PostType.POST_TYPE_EMOJI:
            return EmojiPost(previous=previous)
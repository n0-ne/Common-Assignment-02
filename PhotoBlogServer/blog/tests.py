from django.test import TestCase
from django.contrib.auth import get_user_model
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Post


# Create your tests here.


class PostModelTest(TestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user(
            username="model_tester",
            password="test-password",
        )
        self.post = Post.objects.create(
            author=self.user,
            title="모델 테스트 게시물",
            text="Post 모델 자동 테스트",
        )

    def test_post_string_representation(self):
        self.assertEqual(str(self.post), "모델 테스트 게시물")

    def test_publish_sets_published_date(self):
        self.assertIsNone(self.post.published_date)

        self.post.publish()
        self.post.refresh_from_db()

        self.assertIsNotNone(self.post.published_date)


class PostAPITest(APITestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user(
            username="api_tester",
            password="test-password",
        )
        self.post = Post.objects.create(
            author=self.user,
            title="API GET 테스트 게시물",
            text="JSON Array 응답 자동 검증",
            published_date=timezone.now(),
        )
        self.list_url = reverse("post-list")

    def test_get_post_list_returns_json_array(self):
        response = self.client.get(
            self.list_url,
            HTTP_ACCEPT="application/json",
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]["title"], self.post.title)
        self.assertEqual(response.data[0]["text"], self.post.text)
        self.assertIn("image", response.data[0])

    def test_create_post_through_api(self):
        payload = {
            "author": self.user.pk,
            "title": "API POST 자동 테스트",
            "text": "DRF 테스트 클라이언트로 생성한 게시물",
        }

        response = self.client.post(
            self.list_url,
            payload,
            format="json",
            HTTP_ACCEPT="application/json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Post.objects.count(), 2)
        self.assertEqual(response.data["title"], payload["title"])
        self.assertTrue(
            Post.objects.filter(title=payload["title"]).exists()
        )

    def test_filter_posts_by_title_query(self):
        Post.objects.create(
            author=self.user,
            title="검색 대상 은하 이미지",
            text="제목 검색 API 자동 테스트",
            published_date=timezone.now(),
        )

        response = self.client.get(
            self.list_url,
            {"q": "은하"},
            HTTP_ACCEPT="application/json",
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]["title"], "검색 대상 은하 이미지")

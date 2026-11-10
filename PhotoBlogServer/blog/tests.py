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

    def test_acknowledgement_fields_are_read_only(self):
        detail_url = reverse(
            "post-detail",
            kwargs={"pk": self.post.pk},
        )

        response = self.client.patch(
            detail_url,
            {
                "acknowledged": True,
                "acknowledged_by": self.user.pk,
                "acknowledged_at": timezone.now().isoformat(),
            },
            format="json",
            HTTP_ACCEPT="application/json",
        )

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK,
        )

        self.post.refresh_from_db()
        self.assertFalse(self.post.acknowledged)
        self.assertIsNone(self.post.acknowledged_by)
        self.assertIsNone(self.post.acknowledged_at)

class PostAcknowledgementTest(TestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user(
            username="acknowledger",
            password="test-password",
        )
        self.other_user = get_user_model().objects.create_user(
            username="other_acknowledger",
            password="test-password",
        )
        self.post = Post.objects.create(
            author=self.user,
            title="확인 상태 테스트 게시물",
            text="서버 권한 연동 자동 테스트",
            published_date=timezone.now(),
        )
        self.acknowledge_url = reverse(
            "post_acknowledge",
            kwargs={"pk": self.post.pk},
        )

    def test_anonymous_user_cannot_acknowledge(self):
        response = self.client.post(
            self.acknowledge_url,
        )

        self.assertEqual(response.status_code, 403)

        self.post.refresh_from_db()
        self.assertFalse(self.post.acknowledged)
        self.assertIsNone(self.post.acknowledged_by)
        self.assertIsNone(self.post.acknowledged_at)

    def test_authenticated_user_can_acknowledge(self):
        self.client.force_login(self.user)

        response = self.client.post(
            self.acknowledge_url,
        )

        self.assertRedirects(
            response,
            reverse(
                "post_detail",
                kwargs={"pk": self.post.pk},
            ),
        )

        self.post.refresh_from_db()
        self.assertTrue(self.post.acknowledged)
        self.assertEqual(
            self.post.acknowledged_by,
            self.user,
        )
        self.assertIsNotNone(
            self.post.acknowledged_at,
        )

    def test_repeated_request_preserves_first_acknowledgement(self):
        self.client.force_login(self.user)
        self.client.post(self.acknowledge_url)

        self.post.refresh_from_db()
        first_user = self.post.acknowledged_by
        first_time = self.post.acknowledged_at

        self.client.force_login(self.other_user)
        self.client.post(self.acknowledge_url)

        self.post.refresh_from_db()
        self.assertEqual(
            self.post.acknowledged_by,
            first_user,
        )
        self.assertEqual(
            self.post.acknowledged_at,
            first_time,
        )
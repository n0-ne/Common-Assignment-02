from blog.models import Post
from rest_framework import serializers


class PostSerializer(serializers.ModelSerializer):
    class Meta:
        model = Post
        fields = (
            'author',
            'title',
            'text',
            'created_date',
            'published_date',
            'image',
            'acknowledged',
            'acknowledged_by',
            'acknowledged_at',
        )
        read_only_fields = (
            'acknowledged',
            'acknowledged_by',
            'acknowledged_at',
        )